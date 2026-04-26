package com.schoolmanagement.module.timetable.repository;

import com.schoolmanagement.module.timetable.entity.TimetablePeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TimetablePeriodRepository extends JpaRepository<TimetablePeriod, UUID> {

    List<TimetablePeriod> findBySchoolIdOrderByDisplayOrderAsc(UUID schoolId);

    Optional<TimetablePeriod> findByIdAndSchoolId(UUID id, UUID schoolId);

    boolean existsBySchoolIdAndDisplayOrder(UUID schoolId, Integer displayOrder);

    boolean existsBySchoolIdAndNameIgnoreCase(UUID schoolId, String name);
}