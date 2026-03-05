package com.peer.community.service;

import com.peer.TestUtil;
import com.peer.common.exception.CustomException;
import com.peer.common.exception.ErrorCode;
import com.peer.community.dto.CommentRequest;
import com.peer.community.dto.CommentResponse;
import com.peer.community.dto.PostRequest;
import com.peer.community.dto.PostResponse;
import com.peer.community.dto.ReportRequest;
import com.peer.community.entity.PostTag;
import com.peer.config.TestConfig;
import com.peer.user.entity.User;
import com.peer.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Import(TestConfig.class)
@Transactional
class CommunityIntegrationTest {

    @Autowired private PostService postService;
    @Autowired private CommentService commentService;
    @Autowired private PostLikeService postLikeService;
    @Autowired private ReportService reportService;
    @Autowired private UserRepository userRepository;
    @Autowired private EntityManager em;

    private User author;
    private User reader;
    private Long postId;

    @BeforeEach
    void setUp() {
        author = userRepository.save(User.builder()
                .email("author@hive.fi").name("Author").googleId("g-author").build());
        reader = userRepository.save(User.builder()
                .email("reader@hive.fi").name("Reader").googleId("g-reader").build());

        PostRequest postReq = TestUtil.createDto(PostRequest.class,
                "tag", PostTag.ALGORITHM, "title", "Test Post", "content", "Content here");
        PostResponse post = postService.createPost(postReq, author.getId());
        postId = post.getId();
    }

    // --- Like Tests ---

    @Test
    @DisplayName("Like and unlike a post")
    void likeAndUnlike() {
        postLikeService.like(postId, reader.getId());
        assertThat(postLikeService.isLiked(postId, reader.getId())).isTrue();

        postLikeService.unlike(postId, reader.getId());
        assertThat(postLikeService.isLiked(postId, reader.getId())).isFalse();
    }

    @Test
    @DisplayName("Double like throws ALREADY_LIKED")
    void doubleLike() {
        postLikeService.like(postId, reader.getId());

        assertThatThrownBy(() -> postLikeService.like(postId, reader.getId()))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.ALREADY_LIKED));
    }

    @Test
    @DisplayName("Unlike without liking throws NOT_LIKED")
    void unlikeWithoutLike() {
        assertThatThrownBy(() -> postLikeService.unlike(postId, reader.getId()))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.NOT_LIKED));
    }

    // --- Comment Tests ---

    @Test
    @DisplayName("Create comment and reply with 2-level threading")
    void commentThreading() {
        CommentRequest commentReq = TestUtil.createDto(CommentRequest.class,
                "content", "Top-level comment");
        CommentResponse parent = commentService.createComment(postId, commentReq, reader.getId());

        CommentRequest replyReq = TestUtil.createDto(CommentRequest.class,
                "content", "Reply to comment", "parentId", parent.getId());
        commentService.createComment(postId, replyReq, author.getId());

        em.flush();
        em.clear();

        var comments = commentService.getComments(postId);
        assertThat(comments).hasSize(1); // only top-level
        assertThat(comments.get(0).getReplies()).hasSize(1);
    }

    @Test
    @DisplayName("Reply to a reply attaches to root parent (auto-flattening)")
    void replyToReply_flattens() {
        CommentRequest c1 = TestUtil.createDto(CommentRequest.class, "content", "Root");
        CommentResponse root = commentService.createComment(postId, c1, reader.getId());

        CommentRequest c2 = TestUtil.createDto(CommentRequest.class,
                "content", "Reply", "parentId", root.getId());
        CommentResponse reply = commentService.createComment(postId, c2, author.getId());

        // Reply to the reply — should attach to root
        CommentRequest c3 = TestUtil.createDto(CommentRequest.class,
                "content", "Reply to reply", "parentId", reply.getId());
        commentService.createComment(postId, c3, reader.getId());

        em.flush();
        em.clear();

        var comments = commentService.getComments(postId);
        assertThat(comments).hasSize(1);
        assertThat(comments.get(0).getReplies()).hasSize(2); // both replies under root
    }

    // --- Report Tests ---

    @Test
    @DisplayName("Self-report throws SELF_REPORT")
    void selfReport() {
        assertThatThrownBy(() -> reportService.report(postId, TestUtil.createDto(ReportRequest.class, "reason", "spam"), author.getId()))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.SELF_REPORT));
    }

    @Test
    @DisplayName("Duplicate report throws DUPLICATE_REPORT")
    void duplicateReport() {
        reportService.report(postId, TestUtil.createDto(ReportRequest.class, "reason", "inappropriate"), reader.getId());

        assertThatThrownBy(() -> reportService.report(postId, TestUtil.createDto(ReportRequest.class, "reason", "inappropriate"), reader.getId()))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.DUPLICATE_REPORT));
    }

    // --- Post Authorization ---

    @Test
    @DisplayName("Non-author cannot update post")
    void updatePost_unauthorized() {
        PostRequest updateReq = TestUtil.createDto(PostRequest.class,
                "tag", PostTag.FREE, "title", "Hacked", "content", "Nope");

        assertThatThrownBy(() -> postService.updatePost(postId, updateReq, reader.getId()))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode()).isEqualTo(ErrorCode.UNAUTHORIZED));
    }

    @Test
    @DisplayName("Author can update their own post")
    void updatePost_authorized() {
        PostRequest updateReq = TestUtil.createDto(PostRequest.class,
                "tag", PostTag.DEVELOPMENT, "title", "Updated", "content", "New content");

        PostResponse updated = postService.updatePost(postId, updateReq, author.getId());
        assertThat(updated.getTitle()).isEqualTo("Updated");
    }
}
