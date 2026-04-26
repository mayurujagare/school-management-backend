package com.schoolmanagement.module.fee.repository;

import com.schoolmanagement.module.fee.entity.FeePayment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeePaymentRepository extends JpaRepository<FeePayment, UUID> {

    List<FeePayment> findByLedger_IdOrderByCreatedAtDesc(UUID ledgerId);

    List<FeePayment> findByStudent_IdOrderByPaymentDateDesc(UUID studentId);

    Optional<FeePayment> findBySchoolIdAndReceiptNo(UUID schoolId, String receiptNo);

    boolean existsBySchoolIdAndReceiptNo(UUID schoolId, String receiptNo);

    // Payments collected on a specific date (daily collection report)
    @Query("""
        SELECT p FROM FeePayment p
        JOIN FETCH p.student s
        WHERE p.schoolId = :schoolId
        AND p.paymentDate = :date
        ORDER BY p.createdAt DESC
    """)
    List<FeePayment> findBySchoolAndDate(UUID schoolId, LocalDate date);

    // Total collected in a date range
    @Query("""
        SELECT SUM(p.amountPaid)
        FROM FeePayment p
        WHERE p.schoolId = :schoolId
        AND p.paymentDate BETWEEN :from AND :to
    """)
    java.math.BigDecimal sumCollectedInRange(UUID schoolId,
                                              LocalDate from,
                                              LocalDate to);

    // Latest receipt number for generation
    @Query("""
        SELECT p.receiptNo FROM FeePayment p
        WHERE p.schoolId = :schoolId
        ORDER BY p.createdAt DESC
        LIMIT 1
    """)
    Optional<String> findLatestReceiptNo(UUID schoolId);
}