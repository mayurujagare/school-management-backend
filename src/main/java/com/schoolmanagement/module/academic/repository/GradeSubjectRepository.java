package com.schoolmanagement.module.academic.repository;

import com.schoolmanagement.module.academic.entity.GradeSubject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GradeSubjectRepository extends JpaRepository<GradeSubject, UUID> {

    @Query("""
        SELECT gs FROM GradeSubject gs
        JOIN FETCH gs.subject s
        WHERE gs.grade.id = :gradeId
        ORDER BY s.name ASC
    """)
    List<GradeSubject> findByGradeIdWithSubject(UUID gradeId);

    boolean existsByGrade_IdAndSubject_Id(UUID gradeId, UUID subjectId);

    @Modifying
    @Query("DELETE FROM GradeSubject gs WHERE gs.grade.id = :gradeId AND gs.subject.id = :subjectId")
    void deleteByGradeIdAndSubjectId(UUID gradeId, UUID subjectId);
}