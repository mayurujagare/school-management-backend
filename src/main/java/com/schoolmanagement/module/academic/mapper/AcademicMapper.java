package com.schoolmanagement.module.academic.mapper;

import com.schoolmanagement.module.academic.dto.GradeDto;
import com.schoolmanagement.module.academic.dto.SectionDto;
import com.schoolmanagement.module.academic.dto.SubjectDto;
import com.schoolmanagement.module.academic.entity.Grade;
import com.schoolmanagement.module.academic.entity.GradeSubject;
import com.schoolmanagement.module.academic.entity.Section;
import com.schoolmanagement.module.academic.entity.Subject;
import org.springframework.stereotype.Component;

@Component
public class AcademicMapper {

    public GradeDto.Response toResponse(Grade grade) {
        return GradeDto.Response.builder()
                .id(grade.getId())
                .name(grade.getName())
                .displayOrder(grade.getDisplayOrder())
                .isActive(grade.getIsActive())
                .build();
    }

    public SectionDto.Response toResponse(Section section) {
        return SectionDto.Response.builder()
                .id(section.getId())
                .gradeId(section.getGrade().getId())
                .gradeName(section.getGrade().getName())
                .name(section.getName())
                .capacity(section.getCapacity())
                .isActive(section.getIsActive())
                .build();
    }

    public SubjectDto.Response toResponse(Subject subject) {
        return SubjectDto.Response.builder()
                .id(subject.getId())
                .name(subject.getName())
                .code(subject.getCode())
                .isActive(subject.getIsActive())
                .build();
    }

    public SubjectDto.GradeSubjectResponse toGradeSubjectResponse(GradeSubject gs) {
        return SubjectDto.GradeSubjectResponse.builder()
                .id(gs.getSubject().getId())
                .name(gs.getSubject().getName())
                .code(gs.getSubject().getCode())
                .isMandatory(gs.getIsMandatory())
                .build();
    }
}