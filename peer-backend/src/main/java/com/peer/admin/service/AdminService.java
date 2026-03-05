package com.peer.admin.service;

import com.peer.common.exception.CustomException;
import com.peer.common.exception.ErrorCode;
import com.peer.community.dto.PostResponse;
import com.peer.community.entity.Post;
import com.peer.community.repository.PostRepository;
import com.peer.user.dto.UserResponse;
import com.peer.user.entity.Role;
import com.peer.user.entity.User;
import com.peer.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final UserRepository userRepository;
    private final PostRepository postRepository;

    public Page<UserResponse> getUsers(int page, int size) {
        return userRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                .map(UserResponse::from);
    }

    @Transactional
    public UserResponse promoteToAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        user.promoteToAdmin();
        return UserResponse.from(user);
    }

    @Transactional
    public UserResponse demoteToUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        user.demoteToUser();
        return UserResponse.from(user);
    }

    public Page<PostResponse> getReportedPosts(int page, int size) {
        return postRepository.findByReportCountGreaterThanOrderByReportCountDesc(0, PageRequest.of(page, size))
                .map(PostResponse::from);
    }

    public Page<PostResponse> getBlindedPosts(int page, int size) {
        return postRepository.findByBlindedTrueOrderByCreatedAtDesc(PageRequest.of(page, size))
                .map(PostResponse::from);
    }

    @Transactional
    public PostResponse unblindPost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        post.unblind();
        return PostResponse.from(post);
    }

    @Transactional
    public void deletePost(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));
        postRepository.delete(post);
    }
}
