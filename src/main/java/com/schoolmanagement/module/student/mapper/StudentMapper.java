package com.schoolmanagement.module.student.mapper;

import com.schoolmanagement.module.student.dto.EnrollmentDto;
import com.schoolmanagement.module.student.dto.ParentDto;
import com.schoolmanagement.module.student.dto.StudentDto;
import com.schoolmanagement.module.student.entity.Parent;
import com.schoolmanagement.module.student.entity.Student;
import com.schoolmanagement.module.student.entity.StudentEnrollment;
import com.schoolmanagement.module.student.entity.StudentParentMapping;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class StudentMapper {

    public StudentDto.Response toResponse(Student student) {
        return StudentDto.Response.builder()
                .id(student.getId())
                .admissionNo(student.getAdmissionNo())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .fullName(student.getFullName())
                .dateOfBirth(student.getDateOfBirth())
                .gender(student.getGender())
                .address(student.getAddress())
                .photoUrl(student.getPhotoUrl())
                .bloodGroup(student.getBloodGroup())
                .enrolledAt(student.getEnrolledAt())
                .isActive(student.getIsActive())
                .createdAt(student.getCreatedAt())
                .build();
    }

    public ParentDto.Response toResponse(Parent parent, Boolean isPrimary) {
        return ParentDto.Response.builder()
                .id(parent.getId())
                .firstName(parent.getUser().getFirstName())
                .lastName(parent.getUser().getLastName())
                .fullName(parent.getFullName())
                .email(parent.getEmail())
                .mobile(parent.getMobile())
                .relation(parent.getRelation())
                .occupation(parent.getOccupation())
                .isPrimary(isPrimary)
                .build();
    }

    public EnrollmentDto.Response toResponse(StudentEnrollment enrollment) {
        return EnrollmentDto.Response.builder()
                .id(enrollment.getId())
                .sectionId(enrollment.getSection().getId())
                .sectionName(enrollment.getSection().getName())
                .gradeId(enrollment.getSection().getGrade().getId())
                .gradeName(enrollment.getSection().getGrade().getName())
                .academicYearId(enrollment.getAcademicYear().getId())
                .academicYearLabel(enrollment.getAcademicYear().getLabel())
                .rollNumber(enrollment.getRollNumber())
                .isActive(enrollment.getIsActive())
                .build();
    }

    public ParentDto.StudentSummary toStudentSummary(
            Student student, StudentEnrollment enrollment) {

        return ParentDto.StudentSummary.builder()
                .id(student.getId())
                .admissionNo(student.getAdmissionNo())
                .firstName(student.getFirstName())
                .lastName(student.getLastName())
                .fullName(student.getFullName())
                .photoUrl(student.getPhotoUrl())
                .gradeName(enrollment != null
                        ? enrollment.getSection().getGrade().getName() : null)
                .sectionName(enrollment != null
                        ? enrollment.getSection().getName() : null)
                .build();
    }

    public List<ParentDto.Response> toParentResponses(
            List<StudentParentMapping> mappings) {
        return mappings.stream()
                .map(m -> toResponse(m.getParent(), m.getIsPrimary()))
                .collect(Collectors.toList());
    }
}