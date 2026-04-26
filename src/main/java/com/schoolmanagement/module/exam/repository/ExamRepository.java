package com.schoolmanagement.module.exam.repository;

import com.schoolmanagement.module.exam.entity.Exam;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExamRepository extends JpaRepository<Exam, UUID> {

    @Query("""
        SELECT e FROM Exam e
        JOIN FETCH e.examType et
        JOIN FETCH e.grade g
        JOIN FETCH e.subject s
        WHERE e.schoolId = :schoolId
        AND e.academicYear.id = :academicYearId
        AND (:gradeId IS NULL OR e.grade.id = :gradeId)
        AND (:examTypeId IS NULL OR e.examType.id = :examTypeId)
        ORDER BY e.examDate ASC, s.name ASC
    """)
    List<Exam> findByFilters(UUID schoolId,
                              UUID academicYearId,
                              UUID gradeId,
                              UUID examTypeId);

    @Query("""
        SELECT e FROM Exam e
        JOIN FETCH e.examType et
        JOIN FETCH e.subject s
        WHERE e.schoolId = :schoolId
        AND e.grade.id = :gradeId
        AND e.academicYear.id = :academicYearId
        ORDER BY et.name ASC, s.name ASC
    """)
    List<Exam> findByGradeAndYear(UUID schoolId,
                                   UUID gradeId,
                                   UUID academicYearId);

    Optional<Exam> findByIdAndSchoolId(UUID id, UUID schoolId);

    boolean existsByAcademicYear_IdAndExamType_IdAndGrade_IdAndSubject_Id(
            UUID academicYearId, UUID examTypeId,
            UUID gradeId, UUID subjectId);
}