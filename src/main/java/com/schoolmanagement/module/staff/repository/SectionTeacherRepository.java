package com.schoolmanagement.module.staff.repository;

import com.schoolmanagement.module.staff.entity.SectionTeacher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SectionTeacherRepository extends JpaRepository<SectionTeacher, UUID> {

    // All assignments for a section in an academic year
    @Query("""
        SELECT st FROM SectionTeacher st
        JOIN FETCH st.staff s
        JOIN FETCH s.user u
        LEFT JOIN FETCH st.subject sub
        WHERE st.section.id = :sectionId
        AND st.academicYear.id = :academicYearId
        ORDER BY st.isClassTeacher DESC, u.firstName ASC
    """)
    List<SectionTeacher> findBySectionAndYear(UUID sectionId, UUID academicYearId);

    // All sections a teacher is assigned to
    @Query("""
        SELECT st FROM SectionTeacher st
        JOIN FETCH st.section sec
        JOIN FETCH sec.grade g
        LEFT JOIN FETCH st.subject sub
        WHERE st.staff.id = :staffId
        AND st.academicYear.id = :academicYearId
    """)
    List<SectionTeacher> findByStaffAndYear(UUID staffId, UUID academicYearId);

    // Class teacher of a section
    Optional<SectionTeacher> findBySection_IdAndAcademicYear_IdAndIsClassTeacherTrue(
            UUID sectionId, UUID academicYearId);

    // Check duplicate assignment
    boolean existsBySection_IdAndStaff_IdAndSubject_IdAndAcademicYear_Id(
            UUID sectionId, UUID staffId, UUID subjectId, UUID academicYearId);

    @Modifying
    @Query("""
        DELETE FROM SectionTeacher st
        WHERE st.section.id = :sectionId
        AND st.staff.id = :staffId
        AND st.academicYear.id = :academicYearId
    """)
    void removeAssignment(UUID sectionId, UUID staffId, UUID academicYearId);
}