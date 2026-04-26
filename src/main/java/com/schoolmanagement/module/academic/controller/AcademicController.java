package com.schoolmanagement.module.academic.controller;

import com.schoolmanagement.common.response.ApiResponse;
import com.schoolmanagement.module.academic.dto.GradeDto;
import com.schoolmanagement.module.academic.dto.SectionDto;
import com.schoolmanagement.module.academic.dto.SubjectDto;
import com.schoolmanagement.module.academic.service.GradeService;
import com.schoolmanagement.module.academic.service.SectionService;
import com.schoolmanagement.module.academic.service.SubjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/academic")
@RequiredArgsConstructor
public class AcademicController {

    private final GradeService   gradeService;
    private final SectionService sectionService;
    private final SubjectService subjectService;

    // ─────────────────────────────────────────────────────────
    //  GRADES
    // ─────────────────────────────────────────────────────────

    @PostMapping("/grades")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL')")
    public ResponseEntity<ApiResponse<GradeDto.Response>> createGrade(
            @Valid @RequestBody GradeDto.CreateRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Grade created",
                        gradeService.create(request)));
    }

    @GetMapping("/grades")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','TEACHER','CLERK','PARENT')")
    public ResponseEntity<ApiResponse<List<GradeDto.Response>>> listGrades(
            @RequestParam(defaultValue = "false") boolean includeInactive) {

        return ResponseEntity.ok(
                ApiResponse.success(gradeService.listAll(includeInactive)));
    }

    @GetMapping("/grades/{gradeId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','TEACHER','CLERK')")
    public ResponseEntity<ApiResponse<GradeDto.Response>> getGradeWithSections(
            @PathVariable UUID gradeId) {

        return ResponseEntity.ok(
                ApiResponse.success(gradeService.getWithSections(gradeId)));
    }

    @PatchMapping("/grades/{gradeId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL')")
    public ResponseEntity<ApiResponse<GradeDto.Response>> updateGrade(
            @PathVariable UUID gradeId,
            @Valid @RequestBody GradeDto.UpdateRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success("Grade updated",
                        gradeService.update(gradeId, request)));
    }

    // ─────────────────────────────────────────────────────────
    //  SECTIONS
    // ─────────────────────────────────────────────────────────

    @PostMapping("/sections")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL')")
    public ResponseEntity<ApiResponse<SectionDto.Response>> createSection(
            @Valid @RequestBody SectionDto.CreateRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Section created",
                        sectionService.create(request)));
    }

    @GetMapping("/sections")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','TEACHER','CLERK','PARENT')")
    public ResponseEntity<ApiResponse<List<SectionDto.Response>>> listAllSections() {

        return ResponseEntity.ok(
                ApiResponse.success(sectionService.listAll()));
    }

    @GetMapping("/grades/{gradeId}/sections")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','TEACHER','CLERK','PARENT')")
    public ResponseEntity<ApiResponse<List<SectionDto.Response>>> listSectionsByGrade(
            @PathVariable UUID gradeId) {

        return ResponseEntity.ok(
                ApiResponse.success(sectionService.listByGrade(gradeId)));
    }

    @PatchMapping("/sections/{sectionId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL')")
    public ResponseEntity<ApiResponse<SectionDto.Response>> updateSection(
            @PathVariable UUID sectionId,
            @Valid @RequestBody SectionDto.UpdateRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success("Section updated",
                        sectionService.update(sectionId, request)));
    }

    // ─────────────────────────────────────────────────────────
    //  SUBJECTS
    // ─────────────────────────────────────────────────────────

    @PostMapping("/subjects")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL')")
    public ResponseEntity<ApiResponse<SubjectDto.Response>> createSubject(
            @Valid @RequestBody SubjectDto.CreateRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Subject created",
                        subjectService.create(request)));
    }

    @GetMapping("/subjects")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','TEACHER','CLERK','PARENT')")
    public ResponseEntity<ApiResponse<List<SubjectDto.Response>>> listSubjects(
            @RequestParam(defaultValue = "false") boolean includeInactive) {

        return ResponseEntity.ok(
                ApiResponse.success(subjectService.listAll(includeInactive)));
    }

    @PatchMapping("/subjects/{subjectId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL')")
    public ResponseEntity<ApiResponse<SubjectDto.Response>> updateSubject(
            @PathVariable UUID subjectId,
            @Valid @RequestBody SubjectDto.UpdateRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success("Subject updated",
                        subjectService.update(subjectId, request)));
    }

    // ─────────────────────────────────────────────────────────
    //  GRADE — SUBJECT ASSIGNMENTS
    // ─────────────────────────────────────────────────────────

    @GetMapping("/grades/{gradeId}/subjects")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','TEACHER','CLERK','PARENT')")
    public ResponseEntity<ApiResponse<List<SubjectDto.GradeSubjectResponse>>> listGradeSubjects(
            @PathVariable UUID gradeId) {

        return ResponseEntity.ok(
                ApiResponse.success(subjectService.listByGrade(gradeId)));
    }

    @PostMapping("/grades/{gradeId}/subjects/{subjectId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL')")
    public ResponseEntity<ApiResponse<Void>> assignSubjectToGrade(
            @PathVariable UUID gradeId,
            @PathVariable UUID subjectId,
            @RequestBody(required = false) SubjectDto.AssignToGradeRequest request) {

        if (request == null) request = new SubjectDto.AssignToGradeRequest();
        subjectService.assignToGrade(gradeId, subjectId, request);

        return ResponseEntity.ok(
                ApiResponse.success("Subject assigned to grade"));
    }

    @DeleteMapping("/grades/{gradeId}/subjects/{subjectId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL')")
    public ResponseEntity<ApiResponse<Void>> removeSubjectFromGrade(
            @PathVariable UUID gradeId,
            @PathVariable UUID subjectId) {

        subjectService.removeFromGrade(gradeId, subjectId);
        return ResponseEntity.ok(
                ApiResponse.success("Subject removed from grade"));
    }
}