package com.schoolmanagement.module.exam.controller;

import com.schoolmanagement.common.response.ApiResponse;
import com.schoolmanagement.module.exam.dto.ExamDto;
import com.schoolmanagement.module.exam.dto.ExamTypeDto;
import com.schoolmanagement.module.exam.dto.GradingScaleDto;
import com.schoolmanagement.module.exam.dto.MarkDto;
import com.schoolmanagement.module.exam.service.ExamService;
import com.schoolmanagement.module.exam.service.ExamTypeService;
import com.schoolmanagement.module.exam.service.GradingScaleService;
import com.schoolmanagement.module.exam.service.MarkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/exams")
@RequiredArgsConstructor
public class ExamController {

    private final GradingScaleService gradingScaleService;
    private final ExamTypeService     examTypeService;
    private final ExamService         examService;
    private final MarkService         markService;

    // ─────────────────────────────────────────────────────────
    //  GRADING SCALES
    // ─────────────────────────────────────────────────────────

    @PostMapping("/grading-scales")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL')")
    public ResponseEntity<ApiResponse<GradingScaleDto.Response>> createScale(
            @Valid @RequestBody GradingScaleDto.CreateRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Grading scale created",
                        gradingScaleService.create(request)));
    }

    @GetMapping("/grading-scales")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','TEACHER')")
    public ResponseEntity<ApiResponse<List<GradingScaleDto.Response>>>
            listScales() {

        return ResponseEntity.ok(
                ApiResponse.success(gradingScaleService.listAll()));
    }

    @GetMapping("/grading-scales/{scaleId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','TEACHER')")
    public ResponseEntity<ApiResponse<GradingScaleDto.Response>> getScale(
            @PathVariable UUID scaleId) {

        return ResponseEntity.ok(
                ApiResponse.success(gradingScaleService.getById(scaleId)));
    }

    // ─────────────────────────────────────────────────────────
    //  EXAM TYPES
    // ─────────────────────────────────────────────────────────

    @PostMapping("/types")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL')")
    public ResponseEntity<ApiResponse<ExamTypeDto.Response>> createExamType(
            @Valid @RequestBody ExamTypeDto.CreateRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Exam type created",
                        examTypeService.create(request)));
    }

    @GetMapping("/types")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','TEACHER','CLERK')")
    public ResponseEntity<ApiResponse<List<ExamTypeDto.Response>>> listExamTypes() {

        return ResponseEntity.ok(
                ApiResponse.success(examTypeService.listAll()));
    }

    // ─────────────────────────────────────────────────────────
    //  EXAMS
    // ─────────────────────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL')")
    public ResponseEntity<ApiResponse<ExamDto.Response>> createExam(
            @Valid @RequestBody ExamDto.CreateRequest request) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Exam created",
                        examService.create(request)));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','TEACHER','CLERK','PARENT')")
    public ResponseEntity<ApiResponse<List<ExamDto.Response>>> listExams(
            @RequestParam(required = false) UUID gradeId,
            @RequestParam(required = false) UUID examTypeId) {

        return ResponseEntity.ok(
                ApiResponse.success(examService.listExams(gradeId, examTypeId)));
    }

    @GetMapping("/{examId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','TEACHER','CLERK')")
    public ResponseEntity<ApiResponse<ExamDto.Response>> getExam(
            @PathVariable UUID examId) {

        return ResponseEntity.ok(
                ApiResponse.success(examService.getById(examId)));
    }

    @PatchMapping("/{examId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL')")
    public ResponseEntity<ApiResponse<ExamDto.Response>> updateExam(
            @PathVariable UUID examId,
            @Valid @RequestBody ExamDto.UpdateRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success("Exam updated",
                        examService.update(examId, request)));
    }

    // ─────────────────────────────────────────────────────────
    //  MARKS
    // ─────────────────────────────────────────────────────────

    @PostMapping("/marks")
    @PreAuthorize("hasAnyRole('TEACHER','SCHOOL_ADMIN','PRINCIPAL')")
    public ResponseEntity<ApiResponse<MarkDto.ExamResultSheet>> enterMarks(
            @Valid @RequestBody MarkDto.BulkEnterRequest request) {

        return ResponseEntity.ok(
                ApiResponse.success("Marks entered successfully",
                        markService.bulkEnter(request)));
    }

    @GetMapping("/{examId}/results")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','TEACHER','CLERK')")
    public ResponseEntity<ApiResponse<MarkDto.ExamResultSheet>> getResults(
            @PathVariable UUID examId) {

        return ResponseEntity.ok(
                ApiResponse.success(markService.getExamResultSheet(examId)));
    }

    @GetMapping("/report-card/{studentId}")
    @PreAuthorize("hasAnyRole('SCHOOL_ADMIN','PRINCIPAL','TEACHER','CLERK')")
    public ResponseEntity<ApiResponse<MarkDto.StudentReportCard>> getReportCard(
            @PathVariable UUID studentId,
            @RequestParam(required = false) UUID academicYearId) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        markService.getReportCard(studentId, academicYearId)));
    }

    @GetMapping("/my-child/{studentId}/report-card")
    @PreAuthorize("hasRole('PARENT')")
    public ResponseEntity<ApiResponse<MarkDto.StudentReportCard>> myChildReport(
            @PathVariable UUID studentId,
            @RequestParam(required = false) UUID academicYearId) {

        return ResponseEntity.ok(
                ApiResponse.success(
                        markService.getReportCard(studentId, academicYearId)));
    }
}