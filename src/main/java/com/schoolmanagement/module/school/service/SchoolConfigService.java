package com.schoolmanagement.module.school.service;

import com.schoolmanagement.module.school.dto.SchoolConfigDto;
import com.schoolmanagement.module.school.entity.SchoolConfig;
import com.schoolmanagement.module.school.repository.SchoolConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SchoolConfigService {

    private final SchoolConfigRepository configRepository;

    // ── Get all configs for a school as a map ─────────────────
    public Map<String, String> getAllConfigs(UUID schoolId) {
        return configRepository.findBySchoolId(schoolId)
                .stream()
                .collect(Collectors.toMap(
                        SchoolConfig::getConfigKey,
                        SchoolConfig::getConfigValue));
    }

    // ── Get single config value ───────────────────────────────
    public Optional<String> getConfig(UUID schoolId, String key) {
        return configRepository
                .findBySchoolIdAndConfigKey(schoolId, key)
                .map(SchoolConfig::getConfigValue);
    }

    public String getConfigOrDefault(UUID schoolId, String key, String defaultValue) {
        return getConfig(schoolId, key).orElse(defaultValue);
    }

    // ── Get all configs as response list ──────────────────────
    public List<SchoolConfigDto.Response> getConfigsAsResponse(UUID schoolId) {
        return configRepository.findBySchoolId(schoolId)
                .stream()
                .map(c -> SchoolConfigDto.Response.builder()
                        .key(c.getConfigKey())
                        .value(c.getConfigValue())
                        .build())
                .collect(Collectors.toList());
    }

    // ── Set single config (upsert) ────────────────────────────
    @Transactional
    public void setConfig(UUID schoolId, String key, String value) {
        SchoolConfig config = configRepository
                .findBySchoolIdAndConfigKey(schoolId, key)
                .orElse(SchoolConfig.builder()
                        .schoolId(schoolId)
                        .configKey(key)
                        .build());

        config.setConfigValue(value);
        configRepository.save(config);
    }

    // ── Bulk set configs ──────────────────────────────────────
    @Transactional
    public void bulkSetConfigs(UUID schoolId, Map<String, String> configs) {
        configs.forEach((key, value) -> setConfig(schoolId, key, value));
    }

    // ── Seed defaults for a new school ────────────────────────
    @Transactional
    public void seedDefaultConfigs(UUID schoolId) {
        Map<String, String> defaults = Map.of(
            SchoolConfig.GRADE_LABEL,               "Grade",
            SchoolConfig.SECTION_LABEL,             "Section",
            SchoolConfig.TIMEZONE,                  "Asia/Kolkata",
            SchoolConfig.ACADEMIC_YEAR_START_MONTH, "6",
            SchoolConfig.WORKING_DAYS,              "1,2,3,4,5",
            SchoolConfig.ATTENDANCE_WINDOW_HOURS,   "4",
            SchoolConfig.RECEIPT_PREFIX,            "REC",
            SchoolConfig.SCHOOL_TIMING_START,       "08:00",
            SchoolConfig.SCHOOL_TIMING_END,         "14:00"
        );
        bulkSetConfigs(schoolId, defaults);
    }
}