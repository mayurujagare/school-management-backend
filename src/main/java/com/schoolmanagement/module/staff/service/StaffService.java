package com.schoolmanagement.module.staff.service;

import com.schoolmanagement.common.exception.AppException;
import com.schoolmanagement.common.exception.ErrorCode;
import com.schoolmanagement.common.tenant.TenantContext;
import com.schoolmanagement.module.academic.entity.Section;
import com.schoolmanagement.module.academic.entity.Subject;
import com.schoolmanagement.module.academic.repository.SectionRepository;
import com.schoolmanagement.module.academic.repository.SubjectRepository;
import com.schoolmanagement.module.auth.entity.Role;
import com.schoolmanagement.module.auth.entity.User;
import com.schoolmanagement.module.auth.repository.RoleRepository;
import com.schoolmanagement.module.auth.repository.UserRepository;
import com.schoolmanagement.module.school.entity.AcademicYear;
import com.schoolmanagement.module.school.service.AcademicYearService;
import com.schoolmanagement.module.staff.dto.StaffDto;
import com.schoolmanagement.module.staff.entity.SectionTeacher;
import com.schoolmanagement.module.staff.entity.Staff;
import com.schoolmanagement.module.staff.mapper.StaffMapper;
import com.schoolmanagement.module.staff.repository.SectionTeacherRepository;
import com.schoolmanagement.module.staff.repository.StaffRepository;
import com.schoolmanagement.module.staff.util.EmployeeCodeGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StaffService {

    private final StaffRepository         staffRepository;
    private final SectionTeacherRepository sectionTeacherRepository;
    private final UserRepository          userRepository;
    private final RoleRepository          roleRepository;
    private final SectionRepository       sectionRepository;
    private final SubjectRepository       subjectRepository;
    private final AcademicYearService     academicYearService;
    private final EmployeeCodeGenerator   codeGenerator;
    private final StaffMapper             mapper;
    private final PasswordEncoder         passwordEncoder;

    // ── Create staff member ────────────────────────────────────
    @Transactional
    public StaffDto.Response create(StaffDto.CreateRequest request) {
        UUID schoolId = TenantContext.getTenantId();

        // Contact validation
        if (request.getEmail() == null && request.getMobile() == null) {
            throw new AppException(ErrorCode.VALIDATION_FAILED,
                    "Staff must have at least one contact — email or mobile");
        }

        // Uniqueness checks
        if (request.getEmail() != null &&
                userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.DUPLICATE_ENTRY,
                    "Email already registered");
        }
        if (request.getMobile() != null &&
                userRepository.existsByMobile(request.getMobile())) {
            throw new AppException(ErrorCode.DUPLICATE_ENTRY,
                    "Mobile already registered");
        }

        // Validate role
        Role role = roleRepository.findByName(request.getRole().toUpperCase())
                .orElseThrow(() -> new AppException(ErrorCode.INVALID_REQUEST,
                        "Invalid role: " + request.getRole()));

        // Validate staff roles only
        List<String> staffRoles = List.of(
                Role.TEACHER, Role.CLERK,
                Role.PRINCIPAL, Role.SCHOOL_ADMIN);
        if (!staffRoles.contains(role.getName())) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Role must be one of: TEACHER, CLERK, PRINCIPAL, SCHOOL_ADMIN");
        }

        // Employee code
        String empCode = request.getEmployeeCode();
        if (empCode == null || empCode.isBlank()) {
            empCode = codeGenerator.generate(schoolId);
        } else if (staffRepository.existsBySchoolIdAndEmployeeCode(
                schoolId, empCode)) {
            throw new AppException(ErrorCode.DUPLICATE_ENTRY,
                    "Employee code already in use");
        }

        // Create user account
        User user = User.builder()
                .schoolId(schoolId)
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .email(request.getEmail())
                .mobile(request.getMobile())
                .passwordHash(passwordEncoder.encode("Welcome@123"))
                .authType(User.AuthType.PASSWORD)
                .isActive(true)
                .roles(Set.of(role))
                .build();

        userRepository.save(user);

        // Create staff record
        Staff staff = Staff.builder()
                .schoolId(schoolId)
                .user(user)
                .employeeCode(empCode)
                .designation(request.getDesignation())
                .department(request.getDepartment())
                .dateOfJoining(request.getDateOfJoining())
                .build();

        Staff saved = staffRepository.save(staff);
        log.info("Staff created: {} [{}] role: {}",
                user.getFullName(), empCode, role.getName());

        return mapper.toResponse(saved);
    }

    // ── Search staff ───────────────────────────────────────────
    public Page<StaffDto.Response> search(String search,
                                           Boolean isActive,
                                           Pageable pageable) {
        UUID schoolId = TenantContext.getTenantId();
        boolean active = isActive != null ? isActive : true;

        return staffRepository
                .searchStaff(schoolId, active, search, pageable)
                .map(mapper::toResponse);
    }

    // ── Get staff by ID ────────────────────────────────────────
    public StaffDto.Response getById(UUID staffId) {
        UUID schoolId = TenantContext.getTenantId();
        Staff staff = staffRepository.findByIdAndSchoolId(staffId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Staff not found"));
        return mapper.toResponse(staff);
    }

    // ── Update staff ───────────────────────────────────────────
    @Transactional
    public StaffDto.Response update(UUID staffId,
                                     StaffDto.UpdateRequest request) {
        UUID schoolId = TenantContext.getTenantId();

        Staff staff = staffRepository.findByIdAndSchoolId(staffId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Staff not found"));

        User user = staff.getUser();

        if (request.getFirstName()    != null)
            user.setFirstName(request.getFirstName().trim());
        if (request.getLastName()     != null)
            user.setLastName(request.getLastName().trim());
        if (request.getDesignation()  != null)
            staff.setDesignation(request.getDesignation());
        if (request.getDepartment()   != null)
            staff.setDepartment(request.getDepartment());
        if (request.getDateOfJoining()!= null)
            staff.setDateOfJoining(request.getDateOfJoining());
        if (request.getIsActive()     != null) {
            staff.setIsActive(request.getIsActive());
            user.setIsActive(request.getIsActive());
        }

        userRepository.save(user);
        return mapper.toResponse(staffRepository.save(staff));
    }

    // ── Assign teacher to section ──────────────────────────────
    @Transactional
    public StaffDto.SectionTeacherResponse assignToSection(
            StaffDto.AssignRequest request) {

        UUID schoolId = TenantContext.getTenantId();

        Staff staff = staffRepository
                .findByIdAndSchoolId(request.getStaffId(), schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Staff not found"));

        Section section = sectionRepository
                .findByIdAndSchoolId(request.getSectionId(), schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Section not found"));

        AcademicYear year = request.getAcademicYearId() != null
                ? academicYearService.getCurrentYearEntity(schoolId)
                : academicYearService.getCurrentYearEntity(schoolId);

        Subject subject = null;
        if (request.getSubjectId() != null) {
            subject = subjectRepository
                    .findByIdAndSchoolId(request.getSubjectId(), schoolId)
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                            "Subject not found"));
        }

        // If setting as class teacher — remove existing class teacher first
        if (Boolean.TRUE.equals(request.getIsClassTeacher())) {
            sectionTeacherRepository
                    .findBySection_IdAndAcademicYear_IdAndIsClassTeacherTrue(
                            section.getId(), year.getId())
                    .ifPresent(existing -> {
                        existing.setIsClassTeacher(false);
                        sectionTeacherRepository.save(existing);
                    });
        }

        SectionTeacher assignment = SectionTeacher.builder()
                .schoolId(schoolId)
                .staff(staff)
                .section(section)
                .subject(subject)
                .academicYear(year)
                .isClassTeacher(Boolean.TRUE.equals(request.getIsClassTeacher()))
                .build();

        SectionTeacher saved = sectionTeacherRepository.save(assignment);
        log.info("Staff [{}] assigned to section [{}] subject [{}]",
                staff.getFullName(), section.getName(),
                subject != null ? subject.getName() : "Class Teacher");

        return mapper.toSectionTeacherResponse(saved);
    }

    // ── Get section's teacher list ─────────────────────────────
    public List<StaffDto.SectionTeacherResponse> getSectionTeachers(
            UUID sectionId) {
        UUID schoolId = TenantContext.getTenantId();

        sectionRepository.findByIdAndSchoolId(sectionId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Section not found"));

        AcademicYear year = academicYearService.getCurrentYearEntity(schoolId);

        return sectionTeacherRepository
                .findBySectionAndYear(sectionId, year.getId())
                .stream()
                .map(mapper::toSectionTeacherResponse)
                .collect(Collectors.toList());
    }

    // ── Get teacher's section assignments ──────────────────────
    public List<StaffDto.SectionTeacherResponse> getTeacherAssignments(
            UUID staffId) {
        UUID schoolId = TenantContext.getTenantId();

        staffRepository.findByIdAndSchoolId(staffId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Staff not found"));

        AcademicYear year = academicYearService.getCurrentYearEntity(schoolId);

        return sectionTeacherRepository
                .findByStaffAndYear(staffId, year.getId())
                .stream()
                .map(mapper::toSectionTeacherResponse)
                .collect(Collectors.toList());
    }

    // ── Remove teacher assignment ──────────────────────────────
    @Transactional
    public void removeAssignment(UUID sectionId, UUID staffId) {
        UUID schoolId = TenantContext.getTenantId();

        sectionRepository.findByIdAndSchoolId(sectionId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Section not found"));

        AcademicYear year = academicYearService.getCurrentYearEntity(schoolId);

        sectionTeacherRepository.removeAssignment(
                sectionId, staffId, year.getId());

        log.info("Assignment removed: staff [{}] from section [{}]",
                staffId, sectionId);
    }

    // ── Get my profile (for logged-in staff) ───────────────────
    public StaffDto.Response getMyProfile(UUID userId) {
        UUID schoolId = TenantContext.getTenantId();
        Staff staff = staffRepository
                .findByUser_IdAndSchoolId(userId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Staff profile not found"));
        return mapper.toResponse(staff);
    }
}