package com.schoolmanagement.module.fee.repository;

import com.schoolmanagement.module.fee.entity.StudentFeeLedger;
import com.schoolmanagement.module.fee.entity.StudentFeeLedger.LedgerStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface StudentFeeLedgerRepository
        extends JpaRepository<StudentFeeLedger, UUID> {

    // All ledger entries for a student in an academic year
    @Query("""
        SELECT l FROM StudentFeeLedger l
        JOIN FETCH l.feeStructure fs
        JOIN FETCH fs.feeCategory fc
        LEFT JOIN FETCH l.discount d
        WHERE l.student.id = :studentId
        AND l.academicYear.id = :academicYearId
        ORDER BY l.dueDate ASC
    """)
    List<StudentFeeLedger> findByStudentAndYear(UUID studentId,
                                                UUID academicYearId);

    // Pending/overdue ledger entries for a student
    @Query("""
        SELECT l FROM StudentFeeLedger l
        JOIN FETCH l.feeStructure fs
        JOIN FETCH fs.feeCategory fc
        WHERE l.student.id = :studentId
        AND l.status IN ('PENDING','PARTIAL')
        ORDER BY l.dueDate ASC
    """)
    List<StudentFeeLedger> findPendingByStudent(UUID studentId);

    // All pending fees for a school (for reports)
    @Query("""
        SELECT l FROM StudentFeeLedger l
        JOIN FETCH l.student s
        JOIN FETCH l.feeStructure fs
        JOIN FETCH fs.feeCategory fc
        WHERE l.schoolId = :schoolId
        AND l.academicYear.id = :academicYearId
        AND l.status IN ('PENDING','PARTIAL')
        ORDER BY l.dueDate ASC, s.firstName ASC
    """)
    List<StudentFeeLedger> findAllPendingForSchool(UUID schoolId,
                                                    UUID academicYearId);

    // Overdue entries
    @Query("""
        SELECT l FROM StudentFeeLedger l
        JOIN FETCH l.student s
        WHERE l.schoolId = :schoolId
        AND l.status IN ('PENDING','PARTIAL')
        AND l.dueDate < :today
        ORDER BY l.dueDate ASC
    """)
    List<StudentFeeLedger> findOverdue(UUID schoolId, LocalDate today);

    // Check duplicate ledger entry
    boolean existsByStudent_IdAndFeeStructure_IdAndDueDate(
            UUID studentId, UUID feeStructureId, LocalDate dueDate);

    Optional<StudentFeeLedger> findByIdAndSchoolId(UUID id, UUID schoolId);
}