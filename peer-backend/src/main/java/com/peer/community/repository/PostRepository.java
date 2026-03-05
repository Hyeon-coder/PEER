package com.peer.community.repository;

import com.peer.community.entity.Post;
import com.peer.community.entity.PostTag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, Long> {

    Page<Post> findByBlindedFalseOrderByCreatedAtDesc(Pageable pageable);

    Page<Post> findByTagAndBlindedFalseOrderByCreatedAtDesc(PostTag tag, Pageable pageable);

    Page<Post> findByReportCountGreaterThanOrderByReportCountDesc(int minReports, Pageable pageable);

    Page<Post> findByBlindedTrueOrderByCreatedAtDesc(Pageable pageable);
}
