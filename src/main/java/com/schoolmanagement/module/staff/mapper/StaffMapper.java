package com.schoolmanagement.module.staff.mapper;

import com.schoolmanagement.module.staff.dto.StaffDto;
import com.schoolmanagement.module.staff.entity.SectionTeacher;
import com.schoolmanagement.module.staff.entity.Staff;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class StaffMapper {

    public StaffDto.Response toResponse(Staff staff) {
        return StaffDto.Response.builder()
                .id(staff.getId())
                .userId(staff.getUser().getId())
                .firstName(staff.getUser().getFirstName())
                .lastName(staff.getUser().getLastName())
                .fullName(staff.getFullName())
                .email(staff.getEmail())
                .mobile(staff.getMobile())
                .roles(staff.getUser().getRoles().stream()
                        .map(r -> r.getName())
                        .collect(Collectors.toList()))
                .employeeCode(staff.getEmployeeCode())
                .designation(staff.getDesignation())
                .department(staff.getDepartment())
                .dateOfJoining(staff.getDateOfJoining())
                .isActive(staff.getIsActive())
                .createdAt(staff.getCreatedAt())
                .build();
    }

    public StaffDto.SectionTeacherResponse toSectionTeacherResponse(
            SectionTeacher st) {
        return StaffDto.SectionTeacherResponse.builder()
                .id(st.getId())
                .staffId(st.getStaff().getId())
                .staffName(st.getStaff().getFullName())
                .subjectId(st.getSubject() != null
                        ? st.getSubject().getId() : null)
                .subjectName(st.getSubject() != null
                        ? st.getSubject().getName() : null)
                .sectionId(st.getSection().getId())
                .sectionName(st.getSection().getName())
                .gradeName(st.getSection().getGrade().getName())
                .isClassTeacher(st.getIsClassTeacher())
                .build();
    }
}