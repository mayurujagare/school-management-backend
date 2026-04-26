package com.schoolmanagement.module.student.repository;

import com.schoolmanagement.module.student.entity.Parent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ParentRepository extends JpaRepository<Parent, UUID> {

    Optional<Parent> findByIdAndSchoolId(UUID id, UUID schoolId);

    Optional<Parent> findByUser_Id(UUID userId);

    Optional<Parent> findByUser_IdAndSchoolId(UUID userId, UUID schoolId);

    // Get all parents linked to a student
    @Query("""
        SELECT p FROM Parent p
        JOIN StudentParentMapping m ON m.parent.id = p.id
        WHERE m.student.id = :studentId
        ORDER BY m.isPrimary DESC
    """)
    List<Parent> findByStudentId(UUID studentId);

    // Get all students linked to a parent (for parent dashboard)
    @Query("""
        SELECT DISTINCT p FROM Parent p
        JOIN p.user u
        WHERE p.schoolId = :schoolId
        AND (u.email = :identifier OR u.mobile = :identifier)
    """)
    Optional<Parent> findBySchoolIdAndIdentifier(UUID schoolId, String identifier);
}