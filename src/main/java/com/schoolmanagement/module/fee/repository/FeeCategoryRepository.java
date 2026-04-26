package com.schoolmanagement.module.fee.repository;

import com.schoolmanagement.module.fee.entity.FeeCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeeCategoryRepository extends JpaRepository<FeeCategory, UUID> {

    List<FeeCategory> findBySchoolIdAndIsActiveTrueOrderByNameAsc(UUID schoolId);

    Optional<FeeCategory> findByIdAndSchoolId(UUID id, UUID schoolId);

    boolean existsBySchoolIdAndNameIgnoreCase(UUID schoolId, String name);
}