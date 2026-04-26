package com.schoolmanagement.module.announcement.repository;

import com.schoolmanagement.module.announcement.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeviceTokenRepository extends JpaRepository<DeviceToken, UUID> {

    Optional<DeviceToken> findByUserIdAndToken(UUID userId, String token);

    List<DeviceToken> findByUserIdAndIsActiveTrue(UUID userId);

    // Get all active device tokens for users in a school (for ALL announcements)
    @Query("""
        SELECT dt FROM DeviceToken dt
        JOIN com.schoolmanagement.module.auth.entity.User u ON dt.userId = u.id
        WHERE u.schoolId = :schoolId
        AND dt.isActive = true
    """)
    List<DeviceToken> findActiveTokensBySchoolId(UUID schoolId);

    // Get tokens for parents of students in a specific grade
    @Query("""
        SELECT DISTINCT dt FROM DeviceToken dt
        JOIN com.schoolmanagement.module.student.entity.StudentParentMapping spm
            ON spm.parent.user.id = dt.userId
        JOIN com.schoolmanagement.module.student.entity.StudentEnrollment se
            ON se.student.id = spm.student.id
        WHERE se.section.grade.id = :gradeId
        AND se.isActive = true
        AND dt.isActive = true
    """)
    List<DeviceToken> findActiveTokensByGradeId(UUID gradeId);

    // Get tokens for parents of students in a specific section
    @Query("""
        SELECT DISTINCT dt FROM DeviceToken dt
        JOIN com.schoolmanagement.module.student.entity.StudentParentMapping spm
            ON spm.parent.user.id = dt.userId
        JOIN com.schoolmanagement.module.student.entity.StudentEnrollment se
            ON se.student.id = spm.student.id
        WHERE se.section.id = :sectionId
        AND se.isActive = true
        AND dt.isActive = true
    """)
    List<DeviceToken> findActiveTokensBySectionId(UUID sectionId);

    // Deactivate a specific token (when user logs out or token is stale)
    @Modifying
    @Query("UPDATE DeviceToken dt SET dt.isActive = false WHERE dt.userId = :userId AND dt.token = :token")
    void deactivateToken(UUID userId, String token);

    // Deactivate all tokens for a user (logout from all devices)
    @Modifying
    @Query("UPDATE DeviceToken dt SET dt.isActive = false WHERE dt.userId = :userId")
    void deactivateAllForUser(UUID userId);
}