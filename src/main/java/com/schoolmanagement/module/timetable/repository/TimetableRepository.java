package com.schoolmanagement.module.timetable.repository;

import com.schoolmanagement.module.timetable.entity.Timetable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface TimetableRepository extends JpaRepository<Timetable, UUID> {

    // Weekly timetable for a section (only currently active entries)
    @Query("""
        SELECT t FROM Timetable t
        JOIN FETCH t.period p
        LEFT JOIN FETCH t.subject s
        LEFT JOIN FETCH t.staff st
        LEFT JOIN FETCH st.user u
        WHERE t.section.id = :sectionId
        AND t.academicYear.id = :academicYearId
        AND t.effectiveFrom <= :asOfDate
        AND (t.effectiveUntil IS NULL OR t.effectiveUntil >= :asOfDate)
        ORDER BY t.dayOfWeek ASC, p.displayOrder ASC
    """)
    List<Timetable> findActiveBySectionAndYear(UUID sectionId,
                                               UUID academicYearId,
                                               LocalDate asOfDate);

    // Timetable for a specific day
    @Query("""
        SELECT t FROM Timetable t
        JOIN FETCH t.period p
        LEFT JOIN FETCH t.subject s
        LEFT JOIN FETCH t.staff st
        LEFT JOIN FETCH st.user u
        WHERE t.section.id = :sectionId
        AND t.academicYear.id = :academicYearId
        AND t.dayOfWeek = :dayOfWeek
        AND t.effectiveFrom <= :asOfDate
        AND (t.effectiveUntil IS NULL OR t.effectiveUntil >= :asOfDate)
        ORDER BY p.displayOrder ASC
    """)
    List<Timetable> findBySectionDayAndYear(UUID sectionId,
                                            Short dayOfWeek,
                                            UUID academicYearId,
                                            LocalDate asOfDate);

    // Check if a teacher is already assigned at this day+period
    @Query("""
        SELECT COUNT(t) > 0 FROM Timetable t
        WHERE t.staff.id = :staffId
        AND t.dayOfWeek = :dayOfWeek
        AND t.period.id = :periodId
        AND t.academicYear.id = :academicYearId
        AND t.effectiveFrom <= :asOfDate
        AND (t.effectiveUntil IS NULL OR t.effectiveUntil >= :asOfDate)
        AND t.section.id <> :excludeSectionId
    """)
    boolean isTeacherBusyAtSlot(UUID staffId,
                                Short dayOfWeek,
                                UUID periodId,
                                UUID academicYearId,
                                LocalDate asOfDate,
                                UUID excludeSectionId);

    // Check if section+day+period already has an entry
    @Query("""
        SELECT COUNT(t) > 0 FROM Timetable t
        WHERE t.section.id = :sectionId
        AND t.dayOfWeek = :dayOfWeek
        AND t.period.id = :periodId
        AND t.academicYear.id = :academicYearId
        AND t.effectiveFrom <= :asOfDate
        AND (t.effectiveUntil IS NULL OR t.effectiveUntil >= :asOfDate)
    """)
    boolean existsActiveSlot(UUID sectionId,
                             Short dayOfWeek,
                             UUID periodId,
                             UUID academicYearId,
                             LocalDate asOfDate);

    // Get teacher's schedule for a week
    @Query("""
        SELECT t FROM Timetable t
        JOIN FETCH t.period p
        JOIN FETCH t.section sec
        JOIN FETCH sec.grade g
        LEFT JOIN FETCH t.subject s
        WHERE t.staff.id = :staffId
        AND t.academicYear.id = :academicYearId
        AND t.effectiveFrom <= :asOfDate
        AND (t.effectiveUntil IS NULL OR t.effectiveUntil >= :asOfDate)
        ORDER BY t.dayOfWeek ASC, p.displayOrder ASC
    """)
    List<Timetable> findByStaffAndYear(UUID staffId,
                                       UUID academicYearId,
                                       LocalDate asOfDate);

    // Find specific slot for deactivation/replacement
    @Query("""
        SELECT t FROM Timetable t
        WHERE t.section.id = :sectionId
        AND t.dayOfWeek = :dayOfWeek
        AND t.period.id = :periodId
        AND t.academicYear.id = :academicYearId
        AND t.effectiveFrom <= :asOfDate
        AND (t.effectiveUntil IS NULL OR t.effectiveUntil >= :asOfDate)
    """)
    List<Timetable> findActiveSlot(UUID sectionId,
                                   Short dayOfWeek,
                                   UUID periodId,
                                   UUID academicYearId,
                                   LocalDate asOfDate);
}