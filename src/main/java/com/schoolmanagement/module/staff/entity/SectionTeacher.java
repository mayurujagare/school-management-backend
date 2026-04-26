package com.schoolmanagement.module.staff.entity;

import com.schoolmanagement.module.academic.entity.Section;
import com.schoolmanagement.module.academic.entity.Subject;
import com.schoolmanagement.module.school.entity.AcademicYear;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "section_teachers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SectionTeacher {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "school_id", nullable = false)
    private UUID schoolId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private Section section;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    private Staff staff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subject_id")
    private Subject subject;           // NULL = class teacher

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "academic_year_id", nullable = false)
    private AcademicYear academicYear;

    @Column(name = "is_class_teacher", nullable = false)
    @Builder.Default
    private Boolean isClassTeacher = false;
}