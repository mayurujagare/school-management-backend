package com.schoolmanagement.module.exam.repository;

import com.schoolmanagement.module.exam.entity.StudentMark;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentMarkRepository extends JpaRepository<StudentMark, UUID> {

    Optional<StudentMark> findByExam_IdAndStudent_Id(UUID examId, UUID studentId);

    @Query("""
        SELECT m FROM StudentMark m
        JOIN FETCH m.student s
        WHERE m.exam.id = :examId
        ORDER BY s.firstName ASC
    """)
    List<StudentMark> findByExamId(UUID examId);

    @Query("""
        SELECT m FROM StudentMark m
        JOIN FETCH m.exam e
        JOIN FETCH e.subject s
        JOIN FETCH e.examType et
        WHERE m.student.id = :studentId
        AND e.academicYear.id = :academicYearId
        ORDER BY e.examDate ASC, s.name ASC
    """)
    List<StudentMark> findByStudentAndYear(UUID studentId, UUID academicYearId);

    long countByExam_Id(UUID examId);
}