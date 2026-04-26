package com.schoolmanagement.module.exam.repository;

import com.schoolmanagement.module.exam.entity.GradingScale;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GradingScaleRepository extends JpaRepository<GradingScale, UUID> {

    List<GradingScale> findBySchoolIdAndIsActiveTrueOrderByNameAsc(UUID schoolId);

    Optional<GradingScale> findByIdAndSchoolId(UUID id, UUID schoolId);
}