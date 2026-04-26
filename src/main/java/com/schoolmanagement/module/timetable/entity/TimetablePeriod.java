package com.schoolmanagement.module.timetable.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.UUID;

@Entity
@Table(name = "timetable_periods")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimetablePeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @Column(nullable = false, length = 50)
    private String name;                // 'Period 1', 'Lunch Break', 'Assembly'

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(name = "is_break", nullable = false)
    @Builder.Default
    private Boolean isBreak = false;

    @Column(name = "display_order", nullable = false)
    private Integer displayOrder;

    // ── helper ────────────────────────────────────────────────
    public int getDurationMinutes() {
        return (int) java.time.Duration.between(startTime, endTime).toMinutes();
    }
}