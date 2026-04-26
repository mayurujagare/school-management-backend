package com.schoolmanagement.module.fee.repository;

import com.schoolmanagement.module.fee.entity.FeeStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeeStructureRepository extends JpaRepository<FeeStructure, UUID> {

    @Query("""
        SELECT fs FROM FeeStructure fs
        JOIN FETCH fs.feeCategory fc
        JOIN FETCH fs.grade g
        WHERE fs.schoolId = :schoolId
        AND fs.academicYear.id = :academicYearId
        ORDER BY g.displayOrder ASC, fc.name ASC
    """)
    List<FeeStructure> findBySchoolAndYear(UUID schoolId, UUID academicYearId);

    @Query("""
        SELECT fs FROM FeeStructure fs
        JOIN FETCH fs.feeCategory fc
        WHERE fs.schoolId = :schoolId
        AND fs.grade.id = :gradeId
        AND fs.academicYear.id = :academicYearId
        ORDER BY fc.name ASC
    """)
    List<FeeStructure> findByGradeAndYear(UUID schoolId,
                                           UUID gradeId,
                                           UUID academicYearId);

    Optional<FeeStructure> findByIdAndSchoolId(UUID id, UUID schoolId);

    boolean existsByAcademicYear_IdAndGrade_IdAndFeeCategory_Id(
            UUID academicYearId, UUID gradeId, UUID feeCategoryId);
}