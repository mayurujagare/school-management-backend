package com.schoolmanagement.module.timetable.repository;

import com.schoolmanagement.module.timetable.entity.TimetableException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface TimetableExceptionRepository
        extends JpaRepository<TimetableException, UUID> {

    // All exceptions for a section on a date
    @Query("""
        SELECT te FROM TimetableException te
        JOIN FETCH te.period p
        LEFT JOIN FETCH te.subject s
        LEFT JOIN FETCH te.replacementStaff st
        LEFT JOIN FETCH st.user u
        WHERE te.section.id = :sectionId
        AND te.exceptionDate = :date
        ORDER BY p.displayOrder ASC
    """)
    List<TimetableException> findBySectionAndDate(UUID sectionId,
                                                  LocalDate date);

    // All exceptions for a section in a date range
    @Query("""
        SELECT te FROM TimetableException te
        JOIN FETCH te.period p
        LEFT JOIN FETCH te.subject s
        LEFT JOIN FETCH te.replacementStaff st
        WHERE te.section.id = :sectionId
        AND te.exceptionDate BETWEEN :from AND :to
        ORDER BY te.exceptionDate ASC, p.displayOrder ASC
    """)
    List<TimetableException> findBySectionAndDateRange(UUID sectionId,
                                                       LocalDate from,
                                                       LocalDate to);

    // Check if exception already exists for this slot
    boolean existsBySection_IdAndExceptionDateAndPeriod_Id(
            UUID sectionId, LocalDate date, UUID periodId);
}