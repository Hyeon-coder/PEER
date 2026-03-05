package com.peer.community.entity;

import com.peer.common.entity.BaseTimeEntity;
import com.peer.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "posts")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Post extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PostTag tag;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private int likeCount;

    @Column(nullable = false)
    private int reportCount;

    @Column(nullable = false)
    private boolean blinded;

    @Builder
    public Post(User author, PostTag tag, String title, String content) {
        this.author = author;
        this.tag = tag;
        this.title = title;
        this.content = content;
        this.likeCount = 0;
        this.reportCount = 0;
        this.blinded = false;
    }

    public void update(PostTag tag, String title, String content) {
        this.tag = tag;
        this.title = title;
        this.content = content;
    }

    public void incrementLikeCount() {
        this.likeCount++;
    }

    public void decrementLikeCount() {
        if (this.likeCount > 0) {
            this.likeCount--;
        }
    }

    public void incrementReportCount() {
        this.reportCount++;
    }

    public void blind() {
        this.blinded = true;
    }

    public void unblind() {
        this.blinded = false;
        this.reportCount = 0;
    }
}
