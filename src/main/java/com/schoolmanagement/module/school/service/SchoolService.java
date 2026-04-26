// src/main/java/com/schoolmanagement/module/school/service/SchoolService.java
package com.schoolmanagement.module.school.service;
import com.schoolmanagement.module.superadmin.dto.SuperAdminDto;
import com.schoolmanagement.common.exception.AppException;
import com.schoolmanagement.common.exception.ErrorCode;
import com.schoolmanagement.common.tenant.TenantContext;
import com.schoolmanagement.module.auth.entity.Role;
import com.schoolmanagement.module.auth.entity.User;
import com.schoolmanagement.module.auth.repository.RoleRepository;
import com.schoolmanagement.module.auth.repository.UserRepository;
import com.schoolmanagement.module.school.dto.AcademicYearDto;
import com.schoolmanagement.module.school.dto.SchoolDto;
import com.schoolmanagement.module.school.entity.AcademicYear;
import com.schoolmanagement.module.school.entity.School;
import com.schoolmanagement.module.school.mapper.SchoolMapper;
import com.schoolmanagement.module.school.repository.AcademicYearRepository;
import com.schoolmanagement.module.school.repository.SchoolRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SchoolService {

    private final SchoolRepository      schoolRepository;
    private final AcademicYearRepository academicYearRepository;
    private final UserRepository        userRepository;
    private final RoleRepository        roleRepository;
    private final SchoolConfigService   configService;
    private final SchoolMapper          mapper;
    private final PasswordEncoder       passwordEncoder;

    // ── Super Admin: Onboard New School ──────────────────────
    @Transactional
    public SchoolDto.Response onboardSchool(SchoolDto.CreateRequest request) {

        // Validate uniqueness
        if (request.getEmail() != null &&
                schoolRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.SCHOOL_ALREADY_EXISTS,
                    "A school with this email already exists");
        }

        if (userRepository.existsByEmail(request.getAdminIdentifier()) ||
                userRepository.existsByMobile(request.getAdminIdentifier())) {
            throw new AppException(ErrorCode.DUPLICATE_ENTRY,
                    "Admin identifier already in use");
        }

        // Create school
        School school = School.builder()
                .name(request.getName())
                .address(request.getAddress())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .phone(request.getPhone())
                .email(request.getEmail())
                .website(request.getWebsite())
                .subscriptionPlan(request.getSubscriptionPlan())
                .subscriptionStart(LocalDate.now())
                .subscriptionExpiry(request.getSubscriptionExpiry())
                .build();

        school = schoolRepository.save(school);

        // Seed default configs
        configService.seedDefaultConfigs(school.getId());

        // Create school admin user
        createStaffUser(
            school.getId(),
            request.getAdminFirstName(),
            request.getAdminLastName(),
            request.getAdminIdentifier(),
            Role.SCHOOL_ADMIN
        );

        log.info("School onboarded: {} [{}]", school.getName(), school.getId());
        return mapper.toResponse(school);
    }

    // ── Super Admin: List All Schools ─────────────────────────
    public Page<SchoolDto.Response> listAllSchools(String search,
                                                    Boolean isActive,
                                                    Pageable pageable) {
        return schoolRepository
                .findAllWithFilters(search, isActive, pageable)
                .map(mapper::toResponse);
    }

    // ── Super Admin: Get Any School ───────────────────────────
    public SchoolDto.Response getSchoolById(UUID schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.SCHOOL_NOT_FOUND));
        return mapper.toResponse(school);
    }

    // ── Super Admin: Toggle School Active Status ──────────────
    @Transactional
    public SchoolDto.Response toggleSchoolStatus(UUID schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.SCHOOL_NOT_FOUND));
        school.setIsActive(!school.getIsActive());
        return mapper.toResponse(schoolRepository.save(school));
    }

    // ── Super Admin: Update Subscription ─────────────────────
    @Transactional
    public SchoolDto.Response updateSubscription(UUID schoolId,
                                                  SchoolDto.SubscriptionUpdate request) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.SCHOOL_NOT_FOUND));
        school.setSubscriptionPlan(request.getPlan());
        school.setSubscriptionExpiry(request.getExpiryDate());
        return mapper.toResponse(schoolRepository.save(school));
    }

    // ── School Admin / Principal: Get Own School ──────────────
    public SchoolDto.Response getMySchool() {
        UUID schoolId = TenantContext.getTenantId();
        return getSchoolById(schoolId);
    }

    // ── School Admin / Principal: Update Own School ───────────
    @Transactional
    public SchoolDto.Response updateMySchool(SchoolDto.UpdateRequest request) {
        UUID schoolId = TenantContext.getTenantId();
        School school = schoolRepository.findByIdAndIsActiveTrue(schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.SCHOOL_NOT_FOUND));

        if (request.getName()    != null) school.setName(request.getName());
        if (request.getAddress() != null) school.setAddress(request.getAddress());
        if (request.getCity()    != null) school.setCity(request.getCity());
        if (request.getState()   != null) school.setState(request.getState());
        if (request.getPincode() != null) school.setPincode(request.getPincode());
        if (request.getPhone()   != null) school.setPhone(request.getPhone());
        if (request.getEmail()   != null) school.setEmail(request.getEmail());
        if (request.getWebsite() != null) school.setWebsite(request.getWebsite());
        if (request.getLogoUrl() != null) school.setLogoUrl(request.getLogoUrl());

        return mapper.toResponse(schoolRepository.save(school));
    }

    // ── Platform Stats (Super Admin Dashboard) ────────────────
    public SuperAdminDto.PlatformStats getPlatformStats() {
        List<School> all = schoolRepository.findAll();
        long total    = all.size();
        long active   = all.stream().filter(School::getIsActive).count();
        long demo     = all.stream()
                .filter(s -> s.getSubscriptionPlan() == School.SubscriptionPlan.DEMO)
                .count();
        long expiring = all.stream()
                .filter(s -> s.getSubscriptionExpiry() != null &&
                        !s.getSubscriptionExpiry().isBefore(LocalDate.now()) &&
                        s.getSubscriptionExpiry().isBefore(
                                LocalDate.now().plusDays(30)))
                .count();

        return SuperAdminDto.PlatformStats.builder()
                .totalSchools(total)
                .activeSchools(active)
                .inactiveSchools(total - active)
                .demoSchools(demo)
                .expiringThisMonth(expiring)
                .build();
    }

    // ── Private: Create Staff User ────────────────────────────
    private void createStaffUser(UUID schoolId,
                                  String firstName,
                                  String lastName,
                                  String identifier,
                                  String roleName) {

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new AppException(ErrorCode.INTERNAL_ERROR,
                        "Role not found: " + roleName));

        boolean isEmail = identifier.contains("@");

        User user = User.builder()
                .schoolId(schoolId)
                .firstName(firstName)
                .lastName(lastName)
                .email(isEmail  ? identifier : null)
                .mobile(!isEmail ? identifier : null)
                // Temp password — must change on first login
                .passwordHash(passwordEncoder.encode("Welcome@123"))
                .authType(User.AuthType.PASSWORD)
                .roles(Set.of(role))
                .build();

        userRepository.save(user);
        log.info("Created {} user [{}] for school [{}]",
                roleName, identifier, schoolId);
    }
}