// src/main/java/com/schoolmanagement/module/school/entity/SchoolConfig.java
package com.schoolmanagement.module.school.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "school_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchoolConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(name = "config_key", nullable = false, length = 100)
    private String configKey;

    @Column(name = "config_value", nullable = false)
    private String configValue;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ── Config key constants — avoids magic strings ───────────
    public static final String GRADE_LABEL               = "grade_label";               // 'Grade','Class','Standard'
    public static final String SECTION_LABEL             = "section_label";             // 'Section','Division'
    public static final String TIMEZONE                  = "timezone";                  // 'Asia/Kolkata'
    public static final String ACADEMIC_YEAR_START_MONTH = "academic_year_start_month"; // '6' = June
    public static final String WORKING_DAYS              = "working_days";              // '1,2,3,4,5' Mon-Fri
    public static final String ATTENDANCE_WINDOW_HOURS   = "attendance_window_hours";   // '4'
    public static final String RECEIPT_PREFIX            = "receipt_prefix";            // 'REC','FEE'
    public static final String SCHOOL_TIMING_START       = "school_timing_start";       // '08:00'
    public static final String SCHOOL_TIMING_END         = "school_timing_end";         // '14:00'
}