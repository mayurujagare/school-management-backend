package com.schoolmanagement.module.student.controller;

import com.schoolmanagement.common.response.ApiResponse;
import com.schoolmanagement.module.student.dto.EnrollmentDto;
import com.schoolmanagement.module.student.dto.ParentDto;
import com.schoolmanagement.module.student.dto.StudentDto;
import com.schoolmanagement.module.student.service.ParentService;
import com.schoolmanagement.module.student.service.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/students")
@RequiredArgsConstructor
public class StudentController {

    private final StudentService studentService;
    private final ParentService  parentService;

    // ── Enroll new student ─────────────────────────────────────
    @PostMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','CLERK')")
    public ResponseEntity<ApiResponse<StudentDto.Response>> enroll(
            @Valid @RequestBody StudentDto.EnrollRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Student enrolled successfully",
                        studentService.enroll(request)));
    }

    // ── Get student by ID ──────────────────────────────────────
    @GetMapping("/{studentId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','TEACHER','CLERK')")
    public ResponseEntity<ApiResponse<StudentDto.Response>> getById(
            @PathVariable UUID studentId) {

        return ResponseEntity.ok(
                ApiResponse.success(studentService.getById(studentId)));
    }

    // ── Search students ────────────────────────────────────────
    @GetMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','TEACHER','CLERK')")
    public ResponseEntity<ApiResponse<Page<StudentDto.Response>>> search(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 20) Pageable pageable) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        studentService.search(search, isActive, pageable)));
    }

    // ── Update student ─────────────────────────────────────────
    @PatchMapping("/{studentId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','CLERK')")
    public ResponseEntity<ApiResponse<StudentDto.Response>> update(
            @PathVariable UUID studentId,
            @Valid @RequestBody StudentDto.UpdateRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success("Student updated",
                        studentService.update(studentId, request)));
    }

    // ── Enroll student into section ────────────────────────────
    @PostMapping("/{studentId}/enroll")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','CLERK')")
    public ResponseEntity<ApiResponse<EnrollmentDto.Response>> enrollToSection(
            @PathVariable UUID studentId,
            @Valid @RequestBody StudentDto.EnrollToSectionRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success("Student enrolled into section",
                        studentService.enrollToSection(studentId, request)));
    }

    // ── List students in a section ─────────────────────────────
    @GetMapping("/section/{sectionId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','TEACHER','CLERK')")
    public ResponseEntity<ApiResponse<List<StudentDto.Response>>> listBySection(
            @PathVariable UUID sectionId,
            @RequestParam(required = false) UUID academicYearId) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        studentService.listBySection(sectionId, academicYearId)));
    }

    // ── Add parent to existing student ─────────────────────────
    @PostMapping("/{studentId}/parents")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','CLERK')")
    public ResponseEntity<ApiResponse<StudentDto.Response>> addParent(
            @PathVariable UUID studentId,
            @Valid @RequestBody ParentDto.AddRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success("Parent added successfully",
                        studentService.addParent(studentId, request)));
    }

    // ── Parent dashboard — view own children ───────────────────
    @GetMapping("/my-children")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<ApiResponse<ParentDto.Response>> myChildren(
            @AuthenticationPrincipal UUID currentUserId) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        parentService.getParentDashboard(currentUserId)));
    }
}