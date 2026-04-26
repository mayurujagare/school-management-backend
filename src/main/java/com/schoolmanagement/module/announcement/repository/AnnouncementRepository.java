package com.schoolmanagement.module.announcement.repository;

import com.schoolmanagement.module.announcement.entity.Announcement;
import com.schoolmanagement.module.announcement.entity.Announcement.TargetType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {

    Optional<Announcement> findByIdAndSchoolId(UUID id, UUID schoolId);

    // All announcements for school (admin view — drafts + published)
    @Query("""
        SELECT a FROM Announcement a
        WHERE a.schoolId = :schoolId
        AND (:targetType IS NULL OR a.targetType = :targetType)
        AND (:isDraft IS NULL
             OR (:isDraft = true AND a.publishedAt IS NULL)
             OR (:isDraft = false AND a.publishedAt IS NOT NULL))
        ORDER BY a.createdAt DESC
    """)
    Page<Announcement> findBySchoolWithFilters(UUID schoolId,
                                                TargetType targetType,
                                                Boolean isDraft,
                                                Pageable pageable);

    // Published announcements visible to a parent
    // A parent sees: ALL announcements + announcements for their child's grade + section
    @Query("""
        SELECT DISTINCT a FROM Announcement a
        WHERE a.schoolId = :schoolId
        AND a.publishedAt IS NOT NULL
        AND (
            a.targetType = 'ALL'
            OR (a.targetType = 'GRADE'   AND a.targetId IN :gradeIds)
            OR (a.targetType = 'SECTION' AND a.targetId IN :sectionIds)
        )
        ORDER BY a.publishedAt DESC
    """)
    Page<Announcement> findPublishedForParent(UUID schoolId,
                                               java.util.List<UUID> gradeIds,
                                               java.util.List<UUID> sectionIds,
                                               Pageable pageable);

    // Published announcements for entire school (for ALL target type)
    @Query("""
        SELECT a FROM Announcement a
        WHERE a.schoolId = :schoolId
        AND a.publishedAt IS NOT NULL
        ORDER BY a.publishedAt DESC
    """)
    Page<Announcement> findAllPublished(UUID schoolId, Pageable pageable);

    // Count drafts for dashboard
    long countBySchoolIdAndPublishedAtIsNull(UUID schoolId);
}