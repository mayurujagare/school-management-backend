package com.schoolmanagement.module.announcement.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "announcements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "target_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private TargetType targetType;

    @Column(name = "target_id")
    private UUID targetId;              // grade_id or section_id when targeted

    @Column(name = "published_at")
    private LocalDateTime publishedAt;  // NULL = draft

    @Column(name = "created_by")
    private UUID createdBy;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── helpers ───────────────────────────────────────────────
    public boolean isDraft() {
        return publishedAt == null;
    }

    public boolean isPublished() {
        return publishedAt != null;
    }

    public enum TargetType {
        ALL,        // entire school
        GRADE,      // specific grade (targetId = gradeId)
        SECTION     // specific section (targetId = sectionId)
    }
}