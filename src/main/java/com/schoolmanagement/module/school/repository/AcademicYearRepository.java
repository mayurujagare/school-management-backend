// src/main/java/com/schoolmanagement/module/school/repository/AcademicYearRepository.java
package com.schoolmanagement.module.school.repository;

import com.schoolmanagement.module.school.entity.AcademicYear;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AcademicYearRepository extends JpaRepository<AcademicYear, UUID> {

    List<AcademicYear> findBySchoolIdOrderByStartDateDesc(UUID schoolId);

    Optional<AcademicYear> findBySchoolIdAndIsCurrentTrue(UUID schoolId);

    boolean existsBySchoolIdAndLabel(UUID schoolId, String label);

    // Unset all current flags before setting a new one
    @Modifying
    @Query("UPDATE AcademicYear a SET a.isCurrent = false WHERE a.schoolId = :schoolId")
    void unsetCurrentForSchool(UUID schoolId);
}