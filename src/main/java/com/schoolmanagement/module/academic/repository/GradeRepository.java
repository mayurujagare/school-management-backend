package com.schoolmanagement.module.academic.repository;

import com.schoolmanagement.module.academic.entity.Grade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GradeRepository extends JpaRepository<Grade, UUID> {

    List<Grade> findBySchoolIdAndIsActiveTrueOrderByDisplayOrderAsc(UUID schoolId);

    List<Grade> findBySchoolIdOrderByDisplayOrderAsc(UUID schoolId);

    Optional<Grade> findByIdAndSchoolId(UUID id, UUID schoolId);

    boolean existsBySchoolIdAndNameIgnoreCase(UUID schoolId, String name);

    boolean existsBySchoolIdAndDisplayOrder(UUID schoolId, Integer displayOrder);
}