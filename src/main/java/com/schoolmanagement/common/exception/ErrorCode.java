package com.schoolmanagement.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ── Auth ──────────────────────────────────────────────────
    INVALID_CREDENTIALS        ("AUTH_001", "Invalid credentials",                  HttpStatus.UNAUTHORIZED),
    OTP_INVALID                ("AUTH_002", "Invalid or expired OTP",               HttpStatus.UNAUTHORIZED),
    OTP_MAX_ATTEMPTS           ("AUTH_003", "Maximum OTP attempts exceeded",        HttpStatus.TOO_MANY_REQUESTS),
    OTP_RATE_LIMIT             ("AUTH_004", "Too many OTP requests. Try later",     HttpStatus.TOO_MANY_REQUESTS),
    TOKEN_EXPIRED              ("AUTH_005", "Token has expired",                    HttpStatus.UNAUTHORIZED),
    TOKEN_INVALID              ("AUTH_006", "Token is invalid",                     HttpStatus.UNAUTHORIZED),
    REFRESH_TOKEN_INVALID      ("AUTH_007", "Refresh token is invalid or revoked",  HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED               ("AUTH_008", "Unauthorized access",                  HttpStatus.UNAUTHORIZED),
    FORBIDDEN                  ("AUTH_009", "Access denied",                        HttpStatus.FORBIDDEN),
    USER_NOT_FOUND             ("AUTH_010", "User not found",                       HttpStatus.NOT_FOUND),
    USER_INACTIVE              ("AUTH_011", "User account is inactive",             HttpStatus.FORBIDDEN),
    IDENTIFIER_NOT_FOUND       ("AUTH_012", "No account found with this email/mobile", HttpStatus.NOT_FOUND),

    // ── School ────────────────────────────────────────────────
    SCHOOL_NOT_FOUND           ("SCH_001",  "School not found",                     HttpStatus.NOT_FOUND),
    SCHOOL_INACTIVE            ("SCH_002",  "School subscription is inactive",      HttpStatus.FORBIDDEN),
    SCHOOL_ALREADY_EXISTS      ("SCH_003",  "School already exists",                HttpStatus.CONFLICT),

    // ── Student ───────────────────────────────────────────────
    STUDENT_NOT_FOUND          ("STU_001",  "Student not found",                    HttpStatus.NOT_FOUND),
    ADMISSION_NO_EXISTS        ("STU_002",  "Admission number already exists",      HttpStatus.CONFLICT),

    // ── Parent ────────────────────────────────────────────────
    PARENT_NOT_FOUND           ("PAR_001",  "Parent not found",                     HttpStatus.NOT_FOUND),
    PARENT_LIMIT_EXCEEDED      ("PAR_002",  "A student can have maximum 2 parents", HttpStatus.BAD_REQUEST),
    PARENT_ALREADY_LINKED      ("PAR_003",  "Parent already linked to this student",HttpStatus.CONFLICT),

    // ── Validation ────────────────────────────────────────────
    VALIDATION_FAILED          ("VAL_001",  "Validation failed",                    HttpStatus.BAD_REQUEST),
    DUPLICATE_ENTRY            ("VAL_002",  "Duplicate entry",                      HttpStatus.CONFLICT),
    INVALID_REQUEST            ("VAL_003",  "Invalid request",                      HttpStatus.BAD_REQUEST),

    // ── Generic ───────────────────────────────────────────────
    INTERNAL_ERROR             ("GEN_001",  "Something went wrong. Try again later",HttpStatus.INTERNAL_SERVER_ERROR),
    NOT_FOUND                  ("GEN_002",  "Resource not found",                   HttpStatus.NOT_FOUND);

    private final String     code;
    private final String     message;
    private final HttpStatus httpStatus;
}