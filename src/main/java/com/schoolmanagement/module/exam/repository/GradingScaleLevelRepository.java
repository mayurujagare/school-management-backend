package com.schoolmanagement.module.exam.repository;

import com.schoolmanagement.module.exam.entity.GradingScaleLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GradingScaleLevelRepository
        extends JpaRepository<GradingScaleLevel, UUID> {

    List<GradingScaleLevel> findByGradingScale_IdOrderByDisplayOrderAsc(
            UUID gradingScaleId);
}