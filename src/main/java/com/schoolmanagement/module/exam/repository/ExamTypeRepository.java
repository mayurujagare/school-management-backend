package com.schoolmanagement.module.exam.repository;

import com.schoolmanagement.module.exam.entity.ExamType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExamTypeRepository extends JpaRepository<ExamType, UUID> {

    List<ExamType> findBySchoolIdAndIsActiveTrueOrderByNameAsc(UUID schoolId);

    Optional<ExamType> findByIdAndSchoolId(UUID id, UUID schoolId);

    boolean existsBySchoolIdAndNameIgnoreCase(UUID schoolId, String name);
}