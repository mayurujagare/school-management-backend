package com.schoolmanagement.module.student.entity;

import com.schoolmanagement.module.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "parents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Parent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    private String relation;           // 'FATHER','MOTHER','GUARDIAN'

    @Column(length = 100)
    private String occupation;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ── helpers ───────────────────────────────────────────────
    public String getFullName() {
        return user.getFullName();
    }

    public String getEmail() {
        return user.getEmail();
    }

    public String getMobile() {
        return user.getMobile();
    }
}