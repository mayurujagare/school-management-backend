package com.schoolmanagement.module.school.repository;

import com.schoolmanagement.module.school.entity.SchoolConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SchoolConfigRepository extends JpaRepository<SchoolConfig, UUID> {

    List<SchoolConfig> findBySchoolId(UUID schoolId);

    Optional<SchoolConfig> findBySchoolIdAndConfigKey(UUID schoolId, String configKey);

    @Modifying
    @Query("DELETE FROM SchoolConfig c WHERE c.schoolId = :schoolId AND c.configKey = :key")
    void deleteBySchoolIdAndConfigKey(UUID schoolId, String key);
}