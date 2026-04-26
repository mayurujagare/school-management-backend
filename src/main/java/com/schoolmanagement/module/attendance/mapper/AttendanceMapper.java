package com.schoolmanagement.module.attendance.mapper;

import com.schoolmanagement.module.attendance.dto.AttendanceDto;
import com.schoolmanagement.module.attendance.entity.Attendance;
import org.springframework.stereotype.Component;

@Component
public class AttendanceMapper {

    public AttendanceDto.Response toResponse(Attendance attendance) {
        return AttendanceDto.Response.builder()
                .id(attendance.getId())
                .studentId(attendance.getStudent().getId())
                .studentName(attendance.getStudent().getFullName())
                .admissionNo(attendance.getStudent().getAdmissionNo())
                .date(attendance.getDate())
                .status(attendance.getStatus())
                .remarks(attendance.getRemarks())
                .build();
    }
}