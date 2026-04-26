package com.schoolmanagement.module.student.service;

import com.schoolmanagement.common.exception.AppException;
import com.schoolmanagement.common.exception.ErrorCode;
import com.schoolmanagement.module.auth.entity.Role;
import com.schoolmanagement.module.auth.entity.User;
import com.schoolmanagement.module.auth.repository.RoleRepository;
import com.schoolmanagement.module.auth.repository.UserRepository;
import com.schoolmanagement.module.student.dto.ParentDto;
import com.schoolmanagement.module.student.entity.Parent;
import com.schoolmanagement.module.student.mapper.StudentMapper;
import com.schoolmanagement.module.student.repository.ParentRepository;
import com.schoolmanagement.module.student.repository.StudentEnrollmentRepository;
import com.schoolmanagement.module.student.repository.StudentParentMappingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParentService {

    private final ParentRepository               parentRepository;
    private final UserRepository                 userRepository;
    private final RoleRepository                 roleRepository;
    private final StudentParentMappingRepository mappingRepository;
    private final StudentEnrollmentRepository    enrollmentRepository;
    private final StudentMapper                  mapper;

    // ── Create parent user + parent record ────────────────────
    @Transactional
    public Parent createParent(UUID schoolId,
                                ParentDto.CreateRequest request) {

        // At least one contact required
        if (request.getEmail() == null && request.getMobile() == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "Parent must have at least one contact — email or mobile");
        }

        // Check email uniqueness if provided
        if (request.getEmail() != null &&
                userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.DUPLICATE_ENTRY,
                    "Email '" + request.getEmail() + "' is already registered");
        }

        // Check mobile uniqueness if provided
        if (request.getMobile() != null &&
                userRepository.existsByMobile(request.getMobile())) {
            throw new AppException(ErrorCode.DUPLICATE_ENTRY,
                    "Mobile '" + request.getMobile() + "' is already registered");
        }

        Role parentRole = roleRepository.findByName(Role.PARENT)
                .orElseThrow(() -> new AppException(ErrorCode.INTERNAL_ERROR,
                        "PARENT role not found"));

        // Create user account for parent (OTP login)
        User user = User.builder()
                .schoolId(schoolId)
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .mobile(request.getMobile())
                .authType(User.AuthType.OTP)
                .isActive(true)
                .roles(Set.of(parentRole))
                .build();

        userRepository.save(user);

        // Create parent record
        Parent parent = Parent.builder()
                .schoolId(schoolId)
                .user(user)
                .relation(request.getRelation() != null
                        ? request.getRelation().toUpperCase() : "GUARDIAN")
                .occupation(request.getOccupation())
                .build();

        Parent saved = parentRepository.save(parent);
        log.info("Parent created: {} [{}]", user.getFullName(), saved.getId());
        return saved;
    }

    // ── Get parent dashboard — linked students ─────────────────
    public ParentDto.Response getParentDashboard(UUID userId) {

        Parent parent = parentRepository.findByUser_Id(userId)
                .orElseThrow(() -> new AppException(ErrorCode.PARENT_NOT_FOUND));

        // Get all linked students with their current enrollment
        List<ParentDto.StudentSummary> studentSummaries =
                mappingRepository.findStudentsByParentId(parent.getId())
                        .stream()
                        .map(m -> {
                            // Try to get current enrollment for grade/section info
                            var enrollment = enrollmentRepository
                                    .findByStudent_IdAndAcademicYear_Id(
                                            m.getStudent().getId(),
                                            getCurrentYearId(parent.getSchoolId()))
                                    .orElse(null);
                            return mapper.toStudentSummary(m.getStudent(), enrollment);
                        })
                        .collect(Collectors.toList());

        return ParentDto.Response.builder()
                .id(parent.getId())
                .firstName(parent.getUser().getFirstName())
                .lastName(parent.getUser().getLastName())
                .fullName(parent.getFullName())
                .email(parent.getEmail())
                .mobile(parent.getMobile())
                .relation(parent.getRelation())
                .linkedStudents(studentSummaries)
                .build();
    }

    // ── Helper ─────────────────────────────────────────────────
    private UUID getCurrentYearId(UUID schoolId) {
        // Returns null if no current year set — enrollment lookup returns null
        try {
            return parentRepository
                    .findById(schoolId)
                    .map(p -> p.getSchoolId())
                    .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    // ── Get parent by user ID (for auth context) ───────────────
    public Parent getByUserId(UUID userId) {
        return parentRepository.findByUser_Id(userId)
                .orElseThrow(() -> new AppException(ErrorCode.PARENT_NOT_FOUND));
    }
}