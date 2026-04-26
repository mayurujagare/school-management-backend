package com.schoolmanagement.module.timetable.mapper;

import com.schoolmanagement.module.timetable.dto.TimetableDto;
import com.schoolmanagement.module.timetable.entity.Timetable;
import com.schoolmanagement.module.timetable.entity.TimetableException;
import com.schoolmanagement.module.timetable.entity.TimetablePeriod;
import org.springframework.stereotype.Component;

@Component
public class TimetableMapper {

    public TimetableDto.PeriodResponse toResponse(TimetablePeriod p) {
        return TimetableDto.PeriodResponse.builder()
                .id(p.getId())
                .name(p.getName())
                .startTime(p.getStartTime())
                .endTime(p.getEndTime())
                .isBreak(p.getIsBreak())
                .displayOrder(p.getDisplayOrder())
                .durationMinutes(p.getDurationMinutes())
                .build();
    }

    public TimetableDto.SlotResponse toResponse(Timetable t) {
        return TimetableDto.SlotResponse.builder()
                .id(t.getId())
                .dayOfWeek(t.getDayOfWeek())
                .dayName(t.getDayName())
                .periodId(t.getPeriod().getId())
                .periodName(t.getPeriod().getName())
                .startTime(t.getPeriod().getStartTime())
                .endTime(t.getPeriod().getEndTime())
                .isBreak(t.getPeriod().getIsBreak())
                .subjectId(t.getSubject() != null ? t.getSubject().getId() : null)
                .subjectName(t.getSubject() != null ? t.getSubject().getName() : null)
                .staffId(t.getStaff() != null ? t.getStaff().getId() : null)
                .staffName(t.getStaff() != null ? t.getStaff().getFullName() : null)
                .effectiveFrom(t.getEffectiveFrom())
                .effectiveUntil(t.getEffectiveUntil())
                .build();
    }

    public TimetableDto.ExceptionResponse toResponse(TimetableException te) {
        return TimetableDto.ExceptionResponse.builder()
                .id(te.getId())
                .sectionId(te.getSection().getId())
                .sectionName(te.getSection().getGrade().getName()
                        + " - " + te.getSection().getName())
                .exceptionDate(te.getExceptionDate())
                .periodName(te.getPeriod().getName())
                .startTime(te.getPeriod().getStartTime())
                .endTime(te.getPeriod().getEndTime())
                .exceptionType(te.getExceptionType())
                .subjectName(te.getSubject() != null ? te.getSubject().getName() : null)
                .replacementStaffName(te.getReplacementStaff() != null
                        ? te.getReplacementStaff().getFullName() : null)
                .reason(te.getReason())
                .build();
    }

    public TimetableDto.TeacherSlot toTeacherSlot(Timetable t) {
        return TimetableDto.TeacherSlot.builder()
                .dayOfWeek(t.getDayOfWeek())
                .dayName(t.getDayName())
                .periodName(t.getPeriod().getName())
                .startTime(t.getPeriod().getStartTime())
                .endTime(t.getPeriod().getEndTime())
                .subjectName(t.getSubject() != null ? t.getSubject().getName() : null)
                .gradeName(t.getSection().getGrade().getName())
                .sectionName(t.getSection().getName())
                .build();
    }

    public TimetableDto.EffectiveSlot toEffectiveSlot(Timetable t) {
        return TimetableDto.EffectiveSlot.builder()
                .periodName(t.getPeriod().getName())
                .startTime(t.getPeriod().getStartTime())
                .endTime(t.getPeriod().getEndTime())
                .isBreak(t.getPeriod().getIsBreak())
                .subjectName(t.getSubject() != null ? t.getSubject().getName() : null)
                .staffName(t.getStaff() != null ? t.getStaff().getFullName() : null)
                .isException(false)
                .build();
    }

    public TimetableDto.EffectiveSlot toEffectiveSlot(TimetableException te) {
        return TimetableDto.EffectiveSlot.builder()
                .periodName(te.getPeriod().getName())
                .startTime(te.getPeriod().getStartTime())
                .endTime(te.getPeriod().getEndTime())
                .isBreak(false)
                .subjectName(te.getSubject() != null ? te.getSubject().getName() : null)
                .staffName(te.getReplacementStaff() != null
                        ? te.getReplacementStaff().getFullName() : null)
                .isException(true)
                .exceptionType(te.getExceptionType())
                .reason(te.getReason())
                .build();
    }
}