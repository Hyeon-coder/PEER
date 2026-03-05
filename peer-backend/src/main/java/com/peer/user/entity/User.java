package com.peer.user.entity;

import com.peer.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String name;

    private String profileImageUrl;

    @Column(nullable = false, unique = true)
    private String googleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @Column(nullable = false)
    private Long totalXp;

    @Column(nullable = false)
    private Integer level;

    @Builder
    public User(String email, String name, String profileImageUrl, String googleId) {
        this.email = email;
        this.name = name;
        this.profileImageUrl = profileImageUrl;
        this.googleId = googleId;
        this.role = Role.USER;
        this.totalXp = 0L;
        this.level = 0;
    }

    public void updateProfile(String name, String profileImageUrl) {
        this.name = name;
        this.profileImageUrl = profileImageUrl;
    }

    public void addXp(long xp) {
        this.totalXp += xp;
        this.level = calculateLevel(this.totalXp);
    }

    public void promoteToAdmin() {
        this.role = Role.ADMIN;
    }

    public void demoteToUser() {
        this.role = Role.USER;
    }

    private int calculateLevel(long totalXp) {
        // Level N requires N^2 * 10 cumulative XP
        int lvl = 0;
        while ((long) (lvl + 1) * (lvl + 1) * 10 <= totalXp) {
            lvl++;
        }
        return lvl;
    }
}
