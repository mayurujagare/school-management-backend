package com.schoolmanagement.module.student.service;

import com.schoolmanagement.common.exception.AppException;
import com.schoolmanagement.common.exception.ErrorCode;
import com.schoolmanagement.common.tenant.TenantContext;
import com.schoolmanagement.module.academic.entity.Section;
import com.schoolmanagement.module.academic.repository.SectionRepository;
import com.schoolmanagement.module.school.entity.AcademicYear;
import com.schoolmanagement.module.school.service.AcademicYearService;
import com.schoolmanagement.module.student.dto.EnrollmentDto;
import com.schoolmanagement.module.student.dto.ParentDto;
import com.schoolmanagement.module.student.dto.StudentDto;
import com.schoolmanagement.module.student.entity.Parent;
import com.schoolmanagement.module.student.entity.Student;
import com.schoolmanagement.module.student.entity.StudentEnrollment;
import com.schoolmanagement.module.student.entity.StudentParentMapping;
import com.schoolmanagement.module.student.mapper.StudentMapper;
import com.schoolmanagement.module.student.repository.StudentEnrollmentRepository;
import com.schoolmanagement.module.student.repository.StudentParentMappingRepository;
import com.schoolmanagement.module.student.repository.StudentRepository;
import com.schoolmanagement.module.student.util.AdmissionNumberGenerator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository              studentRepository;
    private final StudentParentMappingRepository mappingRepository;
    private final StudentEnrollmentRepository    enrollmentRepository;
    private final SectionRepository              sectionRepository;
    private final AcademicYearService            academicYearService;
    private final ParentService                  parentService;
    private final AdmissionNumberGenerator       admissionGenerator;
    private final StudentMapper                  mapper;

    // ── Enroll new student ─────────────────────────────────────
    @Transactional
    public StudentDto.Response enroll(StudentDto.EnrollRequest request) {
        UUID schoolId  = TenantContext.getTenantId();
        UUID createdBy = getCurrentUserId();

        // Auto-generate admission number if not provided
        String admissionNo = request.getAdmissionNo();
        if (admissionNo == null || admissionNo.isBlank()) {
            admissionNo = admissionGenerator.generate(schoolId);
        } else {
            // Validate uniqueness of provided admission number
            if (studentRepository.existsBySchoolIdAndAdmissionNo(
                    schoolId, admissionNo)) {
                throw new AppException(ErrorCode.ADMISSION_NO_EXISTS);
            }
        }

        // Create student record
        Student student = Student.builder()
                .schoolId(schoolId)
                .admissionNo(admissionNo)
                .firstName(request.getFirstName().trim())
                .lastName(request.getLastName().trim())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .address(request.getAddress())
                .bloodGroup(request.getBloodGroup())
                .createdBy(createdBy)
                .build();

        student = studentRepository.save(student);
        log.info("Student enrolled: {} {} [{}]",
                student.getFirstName(), student.getLastName(), student.getId());

        // Create and link parents (min 1, max 2 enforced by DTO validation)
        boolean primarySet = false;
        for (ParentDto.CreateRequest parentRequest : request.getParents()) {
            Parent parent = parentService.createParent(schoolId, parentRequest);

            // First parent is primary if none explicitly marked
            boolean isPrimary = Boolean.TRUE.equals(parentRequest.getIsPrimary())
                    || (!primarySet);
            if (isPrimary) primarySet = true;

            StudentParentMapping mapping = StudentParentMapping.builder()
                    .student(student)
                    .parent(parent)
                    .isPrimary(isPrimary)
                    .build();

            mappingRepository.save(mapping);
        }

        // Enroll into section if sectionId provided
        StudentEnrollment enrollment = null;
        if (request.getSectionId() != null) {
            enrollment = enrollIntoSection(student, schoolId,
                    request.getSectionId(), request.getRollNumber(), null);
        }

        return buildFullResponse(student, enrollment);
    }

    // ── Get student by ID ──────────────────────────────────────
    public StudentDto.Response getById(UUID studentId) {
        UUID schoolId = TenantContext.getTenantId();

        Student student = studentRepository
                .findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));

        AcademicYear currentYear = academicYearService
                .getCurrentYearEntity(schoolId);

        StudentEnrollment enrollment = enrollmentRepository
                .findDetailedEnrollment(studentId, currentYear.getId())
                .orElse(null);

        return buildFullResponse(student, enrollment);
    }

    // ── Search / list students ─────────────────────────────────
    public Page<StudentDto.Response> search(String search,
                                             Boolean isActive,
                                             Pageable pageable) {
        UUID schoolId = TenantContext.getTenantId();
        boolean active = isActive != null ? isActive : true;

        return studentRepository
                .searchStudents(schoolId, active, search, pageable)
                .map(s -> buildFullResponse(s, null));
    }

    // ── Update student ─────────────────────────────────────────
    @Transactional
    public StudentDto.Response update(UUID studentId,
                                       StudentDto.UpdateRequest request) {
        UUID schoolId = TenantContext.getTenantId();

        Student student = studentRepository
                .findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));

        if (request.getFirstName()   != null)
            student.setFirstName(request.getFirstName().trim());
        if (request.getLastName()    != null)
            student.setLastName(request.getLastName().trim());
        if (request.getDateOfBirth() != null)
            student.setDateOfBirth(request.getDateOfBirth());
        if (request.getGender()      != null)
            student.setGender(request.getGender());
        if (request.getAddress()     != null)
            student.setAddress(request.getAddress());
        if (request.getBloodGroup()  != null)
            student.setBloodGroup(request.getBloodGroup());
        if (request.getPhotoUrl()    != null)
            student.setPhotoUrl(request.getPhotoUrl());
        if (request.getIsActive()    != null)
            student.setIsActive(request.getIsActive());

        if (request.getAdmissionNo() != null &&
                !request.getAdmissionNo().equals(student.getAdmissionNo())) {
            if (studentRepository.existsBySchoolIdAndAdmissionNo(
                    schoolId, request.getAdmissionNo())) {
                throw new AppException(ErrorCode.ADMISSION_NO_EXISTS);
            }
            student.setAdmissionNo(request.getAdmissionNo());
        }

        return buildFullResponse(studentRepository.save(student), null);
    }

    // ── Enroll student into section ────────────────────────────
    @Transactional
    public EnrollmentDto.Response enrollToSection(
            UUID studentId, StudentDto.EnrollToSectionRequest request) {

        UUID schoolId = TenantContext.getTenantId();

        Student student = studentRepository
                .findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));

        StudentEnrollment enrollment = enrollIntoSection(
                student, schoolId,
                request.getSectionId(),
                request.getRollNumber(),
                request.getAcademicYearId());

        return mapper.toResponse(enrollment);
    }

    // ── List students in a section ─────────────────────────────
    public List<StudentDto.Response> listBySection(UUID sectionId,
                                                    UUID academicYearId) {
        UUID schoolId = TenantContext.getTenantId();

        // Validate section belongs to this school
        sectionRepository.findByIdAndSchoolId(sectionId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Section not found"));

        UUID yearId = academicYearId != null
                ? academicYearId
                : academicYearService.getCurrentYearEntity(schoolId).getId();

        return enrollmentRepository
                .findBySectionAndYear(sectionId, yearId)
                .stream()
                .map(e -> buildFullResponse(e.getStudent(), e))
                .collect(Collectors.toList());
    }

    // ── Add parent to existing student ─────────────────────────
    @Transactional
    public StudentDto.Response addParent(UUID studentId,
                                          ParentDto.AddRequest request) {
        UUID schoolId = TenantContext.getTenantId();

        Student student = studentRepository
                .findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.STUDENT_NOT_FOUND));

        // Max 2 parents check
        long currentCount = mappingRepository.countByStudent_Id(studentId);
        if (currentCount >= 2) {
            throw new AppException(ErrorCode.PARENT_LIMIT_EXCEEDED);
        }

        Parent parent;

        if (request.getExistingParentId() != null) {
            // Link existing parent
            parent = parentService.getByUserId(request.getExistingParentId());

            if (mappingRepository.existsByStudent_IdAndParent_Id(
                    studentId, parent.getId())) {
                throw new AppException(ErrorCode.PARENT_ALREADY_LINKED);
            }
        } else if (request.getNewParent() != null) {
            // Create new parent and link
            parent = parentService.createParent(schoolId, request.getNewParent());
        } else {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Provide either existingParentId or newParent details");
        }

        StudentParentMapping mapping = StudentParentMapping.builder()
                .student(student)
                .parent(parent)
                .isPrimary(Boolean.TRUE.equals(request.getIsPrimary()))
                .build();

        mappingRepository.save(mapping);

        return buildFullResponse(student, null);
    }

    // ── Private: enroll into section ──────────────────────────
    private StudentEnrollment enrollIntoSection(Student student,
                                                 UUID schoolId,
                                                 UUID sectionId,
                                                 String rollNumber,
                                                 UUID academicYearId) {

        Section section = sectionRepository
                .findByIdAndSchoolId(sectionId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Section not found"));

        AcademicYear year = academicYearId != null
                ? academicYearService.getCurrentYearEntity(schoolId)  // fallback
                : academicYearService.getCurrentYearEntity(schoolId);

        // One enrollment per student per academic year
        if (enrollmentRepository.existsByStudent_IdAndAcademicYear_Id(
                student.getId(), year.getId())) {
            throw new AppException(ErrorCode.DUPLICATE_ENTRY,
                    "Student is already enrolled for this academic year");
        }

        // Roll number uniqueness check
        if (rollNumber != null &&
                enrollmentRepository.existsBySection_IdAndAcademicYear_IdAndRollNumber(
                        sectionId, year.getId(), rollNumber)) {
            throw new AppException(ErrorCode.DUPLICATE_ENTRY,
                    "Roll number '" + rollNumber + "' already assigned in this section");
        }

        StudentEnrollment enrollment = StudentEnrollment.builder()
                .schoolId(schoolId)
                .student(student)
                .section(section)
                .academicYear(year)
                .rollNumber(rollNumber)
                .build();

        StudentEnrollment saved = enrollmentRepository.save(enrollment);
        log.info("Student [{}] enrolled in section [{}] for year [{}]",
                student.getFullName(), section.getName(), year.getLabel());

        return saved;
    }

    // ── Private: build full student response ───────────────────
    private StudentDto.Response buildFullResponse(Student student,
                                                   StudentEnrollment enrollment) {
        List<StudentParentMapping> mappings =
                mappingRepository.findByStudent_Id(student.getId());

        StudentDto.Response response = mapper.toResponse(student);
        response.setParents(mapper.toParentResponses(mappings));

        if (enrollment != null) {
            response.setCurrentEnrollment(mapper.toResponse(enrollment));
        }

        return response;
    }

    // ── Private: get current user ID from security context ─────
    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UUID) {
            return (UUID) auth.getPrincipal();
        }
        return null;
    }
}