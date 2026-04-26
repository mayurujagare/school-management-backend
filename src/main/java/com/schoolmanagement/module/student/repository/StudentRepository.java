package com.schoolmanagement.module.student.repository;

import com.schoolmanagement.module.student.entity.Student;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentRepository extends JpaRepository<Student, UUID> {

    Optional<Student> findByIdAndSchoolId(UUID id, UUID schoolId);

    boolean existsBySchoolIdAndAdmissionNo(UUID schoolId, String admissionNo);

    // Search students by name or admission number
    @Query("""
        SELECT s FROM Student s
        WHERE s.schoolId = :schoolId
        AND s.isActive = :isActive
        AND (
            :search IS NULL
            OR LOWER(s.firstName) LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(s.lastName)  LIKE LOWER(CONCAT('%', :search, '%'))
            OR LOWER(s.admissionNo) LIKE LOWER(CONCAT('%', :search, '%'))
        )
        ORDER BY s.firstName ASC, s.lastName ASC
    """)
    Page<Student> searchStudents(UUID schoolId,
                                  Boolean isActive,
                                  String search,
                                  Pageable pageable);

    // Count active students per school
    long countBySchoolIdAndIsActiveTrue(UUID schoolId);

    // Latest admission number for auto-generation
    @Query("""
        SELECT s.admissionNo FROM Student s
        WHERE s.schoolId = :schoolId
        AND s.admissionNo IS NOT NULL
        ORDER BY s.createdAt DESC
        LIMIT 1
    """)
    Optional<String> findLatestAdmissionNo(UUID schoolId);
}