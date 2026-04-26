package com.schoolmanagement.module.fee.repository;

import com.schoolmanagement.module.fee.entity.FeeDiscount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface FeeDiscountRepository extends JpaRepository<FeeDiscount, UUID> {

    List<FeeDiscount> findBySchoolIdAndIsActiveTrueOrderByNameAsc(UUID schoolId);

    Optional<FeeDiscount> findByIdAndSchoolId(UUID id, UUID schoolId);
}