package com.schoolmanagement.module.student.repository;

import com.schoolmanagement.module.student.entity.StudentEnrollment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentEnrollmentRepository
        extends JpaRepository<StudentEnrollment, UUID> {

    // Current enrollment for a student in an academic year
    Optional<StudentEnrollment> findByStudent_IdAndAcademicYear_Id(
            UUID studentId, UUID academicYearId);

    // All enrollments in a section for an academic year
    @Query("""
        SELECT e FROM StudentEnrollment e
        JOIN FETCH e.student s
        WHERE e.section.id = :sectionId
        AND e.academicYear.id = :academicYearId
        AND e.isActive = true
        ORDER BY e.rollNumber ASC, s.firstName ASC
    """)
    List<StudentEnrollment> findBySectionAndYear(UUID sectionId,
                                                  UUID academicYearId);

    // Check if student is already enrolled this year
    boolean existsByStudent_IdAndAcademicYear_Id(UUID studentId,
                                                  UUID academicYearId);

    // Roll number exists in section for academic year
    boolean existsBySection_IdAndAcademicYear_IdAndRollNumber(
            UUID sectionId, UUID academicYearId, String rollNumber);

    // Enrollment with full details — used in attendance & fees
    @Query("""
        SELECT e FROM StudentEnrollment e
        JOIN FETCH e.student s
        JOIN FETCH e.section sec
        JOIN FETCH sec.grade g
        JOIN FETCH e.academicYear ay
        WHERE e.student.id = :studentId
        AND e.academicYear.id = :academicYearId
    """)
    Optional<StudentEnrollment> findDetailedEnrollment(UUID studentId,
                                                        UUID academicYearId);
}