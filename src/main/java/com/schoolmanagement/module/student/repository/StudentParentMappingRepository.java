package com.schoolmanagement.module.student.repository;

import com.schoolmanagement.module.student.entity.StudentParentMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface StudentParentMappingRepository
        extends JpaRepository<StudentParentMapping, UUID> {

    List<StudentParentMapping> findByStudent_Id(UUID studentId);

    List<StudentParentMapping> findByParent_Id(UUID parentId);

    boolean existsByStudent_IdAndParent_Id(UUID studentId, UUID parentId);

    long countByStudent_Id(UUID studentId);

    // All students linked to a parent (for parent mobile app)
    @Query("""
        SELECT m FROM StudentParentMapping m
        JOIN FETCH m.student s
        WHERE m.parent.id = :parentId
        AND s.isActive = true
        ORDER BY m.isPrimary DESC, s.firstName ASC
    """)
    List<StudentParentMapping> findStudentsByParentId(UUID parentId);
}