-- ============================================================
--  SCHOOL MANAGEMENT SYSTEM — FINAL DATABASE SCHEMA
--  Database     : PostgreSQL
--  Hosted on    : Neon.tech
--  Multi-tenant : Shared DB, school_id row-level isolation
--  Version      : 1.0.0
--  Date         : 2026-04-25
-- ============================================================


-- ============================================================
--  EXTENSIONS
-- ============================================================

CREATE EXTENSION IF NOT EXISTS "pgcrypto";   -- gen_random_uuid()


-- ============================================================
--  SECTION 1 — SCHOOLS & CONFIGURATION
-- ============================================================

-- Root tenant table. One row = one paying school client.
CREATE TABLE schools (
    id                    UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    name                  VARCHAR(255)  NOT NULL,
    address               TEXT,
    city                  VARCHAR(100),
    state                 VARCHAR(100),
    country               VARCHAR(100)  DEFAULT 'India',
    pincode               VARCHAR(10),
    phone                 VARCHAR(20),
    email                 VARCHAR(255),
    logo_url              TEXT,
    website               VARCHAR(255),
    is_active             BOOLEAN       NOT NULL DEFAULT TRUE,
    subscription_plan     VARCHAR(50),                         -- 'DEMO','BASIC','STANDARD','PREMIUM'
    subscription_start    DATE,
    subscription_expiry   DATE,
    created_at            TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- Flexible key-value configuration store per school.
-- Stores settings like timezone, academic_year_start_month,
-- attendance_window_hours, grade_label ('Grade'/'Class'/'Standard'), etc.
CREATE TABLE school_configs (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id     UUID          NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    config_key    VARCHAR(100)  NOT NULL,
    config_value  TEXT          NOT NULL,
    created_at    TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP     NOT NULL DEFAULT NOW(),
    UNIQUE (school_id, config_key)
);

-- Academic years per school. Only one can be current at a time.
CREATE TABLE academic_years (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id     UUID          NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    label         VARCHAR(50)   NOT NULL,      -- e.g. '2024-25'
    start_date    DATE          NOT NULL,
    end_date      DATE          NOT NULL,
    is_current    BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMP     NOT NULL DEFAULT NOW(),
    UNIQUE (school_id, label)
);


-- ============================================================
--  SECTION 2 — USERS, ROLES & AUTH
-- ============================================================

-- Single unified users table for all human actors.
-- Students do NOT have a user record (no login for students).
-- password_hash is NULL for parents (OTP only).
-- email/mobile — at least one is mandatory (enforced by CHECK).
CREATE TABLE users (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID          REFERENCES schools(id) ON DELETE CASCADE,
                                  -- NULL only for SUPER_ADMIN
    first_name      VARCHAR(100)  NOT NULL,
    last_name       VARCHAR(100)  NOT NULL,
    email           VARCHAR(255)  UNIQUE,
    mobile          VARCHAR(20)   UNIQUE,
    password_hash   VARCHAR(255),              -- NULL for parents (OTP-only auth)
    auth_type       VARCHAR(10)   NOT NULL DEFAULT 'OTP',
                                  -- 'OTP' = parents
                                  -- 'PASSWORD' = staff roles
    is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
    last_login      TIMESTAMP,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_user_contact CHECK (email IS NOT NULL OR mobile IS NOT NULL)
);

-- Seed roles: SUPER_ADMIN, SCHOOL_ADMIN, PRINCIPAL, TEACHER, CLERK, PARENT
CREATE TABLE roles (
    id    UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    name  VARCHAR(50)  UNIQUE NOT NULL
);

-- Many-to-many. Supports future multi-role users.
CREATE TABLE user_roles (
    user_id   UUID  NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id   UUID  NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- OTP records for parent login flow.
-- One row per OTP request. Marked used after successful verify.
CREATE TABLE otp_verifications (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    identifier   VARCHAR(255) NOT NULL,   -- email or mobile supplied at login
    otp_code     VARCHAR(6)   NOT NULL,
    purpose      VARCHAR(30)  NOT NULL,   -- 'LOGIN' | 'PASSWORD_RESET'
    is_used      BOOLEAN      NOT NULL DEFAULT FALSE,
    attempts     SMALLINT     NOT NULL DEFAULT 0,   -- max 3 wrong attempts
    expires_at   TIMESTAMP    NOT NULL,             -- 10 minutes from created_at
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

-- Refresh tokens — one per active device session.
-- Revocable server-side.
CREATE TABLE refresh_tokens (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id       UUID          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash    VARCHAR(255)  NOT NULL UNIQUE,
    expires_at    TIMESTAMP     NOT NULL,           -- 7 days
    is_revoked    BOOLEAN       NOT NULL DEFAULT FALSE,
    device_info   VARCHAR(255),                     -- optional UA string
    created_at    TIMESTAMP     NOT NULL DEFAULT NOW()
);


-- ============================================================
--  SECTION 3 — ACADEMIC STRUCTURE
-- ============================================================

-- Grades are fully configurable per school.
-- A school may call them Grade, Class, Standard, Form, etc.
-- (stored in school_configs as 'grade_label')
CREATE TABLE grades (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID          NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    name            VARCHAR(50)   NOT NULL,        -- 'Grade 1', 'Nursery', 'KG', 'Class 10'
    display_order   INT           NOT NULL,         -- used for sorting in UI
    is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    UNIQUE (school_id, name)
);

-- Sections within a grade (A, B, C …)
CREATE TABLE sections (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id   UUID          NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    grade_id    UUID          NOT NULL REFERENCES grades(id) ON DELETE CASCADE,
    name        VARCHAR(10)   NOT NULL,             -- 'A', 'B', 'C'
    capacity    INT,
    is_active   BOOLEAN       NOT NULL DEFAULT TRUE,
    UNIQUE (grade_id, name)
);

-- Subjects are school-level. Assigned to grades via grade_subjects.
CREATE TABLE subjects (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id   UUID          NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    name        VARCHAR(100)  NOT NULL,
    code        VARCHAR(20),
    is_active   BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW(),
    UNIQUE (school_id, code)
);

-- Which subjects are taught in which grade.
CREATE TABLE grade_subjects (
    id            UUID     PRIMARY KEY DEFAULT gen_random_uuid(),
    grade_id      UUID     NOT NULL REFERENCES grades(id) ON DELETE CASCADE,
    subject_id    UUID     NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    is_mandatory  BOOLEAN  NOT NULL DEFAULT TRUE,
    UNIQUE (grade_id, subject_id)
);


-- ============================================================
--  SECTION 4 — STAFF
-- ============================================================

-- Covers Teachers, Clerks, School Admins, Principal.
-- All are users first; staff table holds school-employment data.
CREATE TABLE staff (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id         UUID          NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    user_id           UUID          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    employee_code     VARCHAR(50),
    designation       VARCHAR(100),
    department        VARCHAR(100),
    date_of_joining   DATE,
    is_active         BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP     NOT NULL DEFAULT NOW(),
    UNIQUE (school_id, employee_code)
);

-- Which staff teaches which subject in which section for an academic year.
-- is_class_teacher = TRUE means this is the homeroom/class teacher.
CREATE TABLE section_teachers (
    id                UUID     PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id         UUID     NOT NULL REFERENCES schools(id),
    section_id        UUID     NOT NULL REFERENCES sections(id) ON DELETE CASCADE,
    staff_id          UUID     NOT NULL REFERENCES staff(id) ON DELETE CASCADE,
    subject_id        UUID     REFERENCES subjects(id),   -- NULL = class teacher (no subject)
    academic_year_id  UUID     NOT NULL REFERENCES academic_years(id),
    is_class_teacher  BOOLEAN  NOT NULL DEFAULT FALSE,
    UNIQUE (section_id, subject_id, academic_year_id)
);


-- ============================================================
--  SECTION 5 — STUDENTS & PARENTS
-- ============================================================

-- Students have NO login. They are viewed via parent account.
-- No user_id FK here.
CREATE TABLE students (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID          NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    admission_no    VARCHAR(50),
    first_name      VARCHAR(100)  NOT NULL,
    last_name       VARCHAR(100)  NOT NULL,
    date_of_birth   DATE,
    gender          VARCHAR(10),                   -- 'MALE','FEMALE','OTHER'
    address         TEXT,
    photo_url       TEXT,
    blood_group     VARCHAR(5),
    enrolled_at     DATE          NOT NULL DEFAULT CURRENT_DATE,
    is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_by      UUID          REFERENCES users(id),   -- clerk/admin/principal who enrolled
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    UNIQUE (school_id, admission_no)
);

-- Parents have login (OTP). One parent user can be linked to multiple students.
CREATE TABLE parents (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id   UUID          NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    user_id     UUID          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    relation    VARCHAR(20)   NOT NULL,   -- 'FATHER','MOTHER','GUARDIAN'
    occupation  VARCHAR(100),
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW(),
    UNIQUE (school_id, user_id)
);

-- Links students to parents. Min 1, Max 2 parents per student.
-- is_primary = TRUE flags the main contact parent.
CREATE TABLE student_parent_mapping (
    id          UUID     PRIMARY KEY DEFAULT gen_random_uuid(),
    student_id  UUID     NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    parent_id   UUID     NOT NULL REFERENCES parents(id) ON DELETE CASCADE,
    is_primary  BOOLEAN  NOT NULL DEFAULT FALSE,
    UNIQUE (student_id, parent_id)
);

-- Student's class assignment per academic year.
-- One active enrollment per student per year.
CREATE TABLE student_enrollments (
    id                UUID     PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id         UUID     NOT NULL REFERENCES schools(id),
    student_id        UUID     NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    section_id        UUID     NOT NULL REFERENCES sections(id),
    academic_year_id  UUID     NOT NULL REFERENCES academic_years(id),
    roll_number       VARCHAR(20),
    is_active         BOOLEAN  NOT NULL DEFAULT TRUE,
    promoted_from     UUID     REFERENCES student_enrollments(id),   -- previous year ref
    created_at        TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (student_id, academic_year_id)
);


-- ============================================================
--  SECTION 6 — TIMETABLE
-- ============================================================

-- School-level time slots (Period 1, Period 2, Lunch, etc.)
CREATE TABLE timetable_periods (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID          NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    name            VARCHAR(50)   NOT NULL,     -- 'Period 1', 'Lunch Break', 'Assembly'
    start_time      TIME          NOT NULL,
    end_time        TIME          NOT NULL,
    is_break        BOOLEAN       NOT NULL DEFAULT FALSE,
    display_order   INT           NOT NULL
);

-- Weekly recurring timetable.
-- effective_from / effective_until allow versioned schedule changes.
CREATE TABLE timetable (
    id                UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id         UUID      NOT NULL REFERENCES schools(id),
    section_id        UUID      NOT NULL REFERENCES sections(id) ON DELETE CASCADE,
    academic_year_id  UUID      NOT NULL REFERENCES academic_years(id),
    day_of_week       SMALLINT  NOT NULL,     -- 1=Monday … 6=Saturday (no Sundays)
    period_id         UUID      NOT NULL REFERENCES timetable_periods(id),
    subject_id        UUID      REFERENCES subjects(id),
    staff_id          UUID      REFERENCES staff(id),
    effective_from    DATE      NOT NULL,
    effective_until   DATE,                   -- NULL = currently active
    UNIQUE (section_id, day_of_week, period_id, effective_from)
);

-- One-off overrides to the recurring timetable.
-- Used for substitutions, cancellations, extra classes, events.
CREATE TABLE timetable_exceptions (
    id                     UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id              UUID          NOT NULL REFERENCES schools(id),
    original_timetable_id  UUID          REFERENCES timetable(id),
    section_id             UUID          NOT NULL REFERENCES sections(id),
    exception_date         DATE          NOT NULL,
    period_id              UUID          NOT NULL REFERENCES timetable_periods(id),
    exception_type         VARCHAR(20)   NOT NULL,   -- 'SUBSTITUTION','CANCELLATION','EXTRA'
    subject_id             UUID          REFERENCES subjects(id),
    replacement_staff_id   UUID          REFERENCES staff(id),
    reason                 TEXT,
    created_by             UUID          REFERENCES users(id),
    created_at             TIMESTAMP     NOT NULL DEFAULT NOW()
);


-- ============================================================
--  SECTION 7 — ATTENDANCE
-- ============================================================

CREATE TABLE attendance (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id         UUID          NOT NULL REFERENCES schools(id),
    student_id        UUID          NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    section_id        UUID          NOT NULL REFERENCES sections(id),
    academic_year_id  UUID          NOT NULL REFERENCES academic_years(id),
    date              DATE          NOT NULL,
    status            VARCHAR(10)   NOT NULL,   -- 'PRESENT','ABSENT','LATE','HOLIDAY','LEAVE'
    marked_by         UUID          REFERENCES users(id),
    remarks           VARCHAR(255),
    created_at        TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP     NOT NULL DEFAULT NOW(),
    UNIQUE (student_id, date)
);


-- ============================================================
--  SECTION 8 — FEE MANAGEMENT
-- ============================================================

-- Fee categories: Tuition, Transport, Library, Lab, etc.
CREATE TABLE fee_categories (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id   UUID          NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    name        VARCHAR(100)  NOT NULL,
    description TEXT,
    is_active   BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- Fee structure: what a grade owes per category per academic year.
-- Fully configurable per school.
CREATE TABLE fee_structures (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id         UUID          NOT NULL REFERENCES schools(id),
    academic_year_id  UUID          NOT NULL REFERENCES academic_years(id),
    grade_id          UUID          NOT NULL REFERENCES grades(id),
    fee_category_id   UUID          NOT NULL REFERENCES fee_categories(id),
    amount            NUMERIC(10,2) NOT NULL,
    frequency         VARCHAR(20)   NOT NULL,   -- 'MONTHLY','QUARTERLY','ANNUAL','ONE_TIME'
    due_day           INT,                       -- day of month the fee is due (for MONTHLY)
    created_at        TIMESTAMP     NOT NULL DEFAULT NOW(),
    UNIQUE (academic_year_id, grade_id, fee_category_id)
);

-- Named discounts. Applied per student on the ledger.
CREATE TABLE fee_discounts (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID          NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    name            VARCHAR(100)  NOT NULL,   -- 'Sibling Discount','Staff Ward','Merit'
    discount_type   VARCHAR(10)   NOT NULL,   -- 'PERCENT' | 'FIXED'
    value           NUMERIC(10,2) NOT NULL,
    is_active       BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- What each student owes — generated from fee_structures.
-- One row per student per fee-structure entry per due date.
CREATE TABLE student_fee_ledger (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id         UUID          NOT NULL REFERENCES schools(id),
    student_id        UUID          NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    fee_structure_id  UUID          NOT NULL REFERENCES fee_structures(id),
    academic_year_id  UUID          NOT NULL REFERENCES academic_years(id),
    due_date          DATE          NOT NULL,
    amount_due        NUMERIC(10,2) NOT NULL,
    discount_id       UUID          REFERENCES fee_discounts(id),
    discount_amount   NUMERIC(10,2) NOT NULL DEFAULT 0,
    net_amount        NUMERIC(10,2) NOT NULL,   -- amount_due - discount_amount
    amount_paid       NUMERIC(10,2) NOT NULL DEFAULT 0,
    status            VARCHAR(20)   NOT NULL DEFAULT 'PENDING',
                                               -- 'PENDING','PARTIAL','PAID','WAIVED'
    created_at        TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at        TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- Every offline payment recorded here. Each record = one receipt.
-- receipt_no is school-scoped and unique per school.
CREATE TABLE fee_payments (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id       UUID          NOT NULL REFERENCES schools(id),
    student_id      UUID          NOT NULL REFERENCES students(id),
    ledger_id       UUID          NOT NULL REFERENCES student_fee_ledger(id),
    receipt_no      VARCHAR(50)   NOT NULL,
    amount_paid     NUMERIC(10,2) NOT NULL,
    payment_mode    VARCHAR(20)   NOT NULL,   -- 'CASH','CHEQUE','UPI','BANK_TRANSFER','DD'
    payment_date    DATE          NOT NULL,
    cheque_no       VARCHAR(50),              -- populated when payment_mode = 'CHEQUE'
    bank_name       VARCHAR(100),
    collected_by    UUID          NOT NULL REFERENCES users(id),   -- clerk/admin/principal
    remarks         TEXT,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    UNIQUE (school_id, receipt_no)
);


-- ============================================================
--  SECTION 9 — GRADING & EXAMS
-- ============================================================

-- Configurable grading scales per school.
-- e.g. A+/A/B+… or Distinction/Merit/Pass/Fail or 10-point GPA
CREATE TABLE grading_scales (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id   UUID          NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    name        VARCHAR(100)  NOT NULL,
    is_active   BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW()
);

CREATE TABLE grading_scale_levels (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    grading_scale_id  UUID          NOT NULL REFERENCES grading_scales(id) ON DELETE CASCADE,
    grade_label       VARCHAR(10)   NOT NULL,     -- 'A+','A','B+','Pass','Fail'
    min_percentage    NUMERIC(5,2)  NOT NULL,
    max_percentage    NUMERIC(5,2)  NOT NULL,
    grade_point       NUMERIC(4,2),               -- optional GPA value
    description       VARCHAR(100),
    display_order     INT           NOT NULL
);

-- Exam types: Unit Test, Mid-term, Final, Assignment, etc.
CREATE TABLE exam_types (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id   UUID          NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    name        VARCHAR(100)  NOT NULL,
    max_marks   NUMERIC(6,2)  NOT NULL,
    weightage   NUMERIC(5,2),                     -- % contribution to final result
    is_active   BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- Exam schedule. One row per subject per grade per exam type.
CREATE TABLE exams (
    id                UUID      PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id         UUID      NOT NULL REFERENCES schools(id),
    academic_year_id  UUID      NOT NULL REFERENCES academic_years(id),
    exam_type_id      UUID      NOT NULL REFERENCES exam_types(id),
    grade_id          UUID      NOT NULL REFERENCES grades(id),
    subject_id        UUID      NOT NULL REFERENCES subjects(id),
    exam_date         DATE,
    start_time        TIME,
    duration_minutes  INT,
    grading_scale_id  UUID      REFERENCES grading_scales(id),
    created_by        UUID      REFERENCES users(id),
    created_at        TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Marks per student per exam.
CREATE TABLE student_marks (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_id         UUID          NOT NULL REFERENCES exams(id) ON DELETE CASCADE,
    student_id      UUID          NOT NULL REFERENCES students(id) ON DELETE CASCADE,
    marks_obtained  NUMERIC(6,2),                 -- NULL if absent
    is_absent       BOOLEAN       NOT NULL DEFAULT FALSE,
    remarks         TEXT,
    entered_by      UUID          REFERENCES users(id),
    entered_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    UNIQUE (exam_id, student_id)
);


-- ============================================================
--  SECTION 10 — COMMUNICATION & NOTIFICATIONS
-- ============================================================

-- One-way: School → Parent (no reply, no threads).
-- target_type drives who receives the announcement.
CREATE TABLE announcements (
    id            UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id     UUID          NOT NULL REFERENCES schools(id) ON DELETE CASCADE,
    title         VARCHAR(255)  NOT NULL,
    body          TEXT          NOT NULL,
    target_type   VARCHAR(20)   NOT NULL,   -- 'ALL','GRADE','SECTION'
    target_id     UUID,                     -- grade_id or section_id when targeted
    published_at  TIMESTAMP,                -- NULL = draft, set = published
    created_by    UUID          REFERENCES users(id),
    created_at    TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMP     NOT NULL DEFAULT NOW()
);

-- Push notification device tokens for the parent mobile app.
-- One parent may have multiple devices.
CREATE TABLE device_tokens (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID          NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       TEXT          NOT NULL,
    platform    VARCHAR(10)   NOT NULL,   -- 'ANDROID' | 'IOS'
    is_active   BOOLEAN       NOT NULL DEFAULT TRUE,
    updated_at  TIMESTAMP     NOT NULL DEFAULT NOW(),
    created_at  TIMESTAMP     NOT NULL DEFAULT NOW(),
    UNIQUE (user_id, token)
);


-- ============================================================
--  SECTION 11 — INDEXES
--  Ordered: tenant isolation first, then query-pattern based
-- ============================================================

-- Schools
CREATE INDEX idx_schools_active               ON schools(is_active);

-- Users
CREATE INDEX idx_users_school                 ON users(school_id);
CREATE INDEX idx_users_email                  ON users(email);
CREATE INDEX idx_users_mobile                 ON users(mobile);

-- OTP
CREATE INDEX idx_otp_identifier_active        ON otp_verifications(identifier, is_used, expires_at);

-- Refresh tokens
CREATE INDEX idx_refresh_tokens_user          ON refresh_tokens(user_id, is_revoked);

-- Academic years
CREATE INDEX idx_academic_years_school        ON academic_years(school_id, is_current);

-- Grades & sections
CREATE INDEX idx_grades_school                ON grades(school_id);
CREATE INDEX idx_sections_grade               ON sections(grade_id);

-- Staff
CREATE INDEX idx_staff_school                 ON staff(school_id);
CREATE INDEX idx_staff_user                   ON staff(user_id);

-- Students
CREATE INDEX idx_students_school              ON students(school_id, is_active);
CREATE INDEX idx_students_admission           ON students(school_id, admission_no);

-- Parents
CREATE INDEX idx_parents_school               ON parents(school_id);
CREATE INDEX idx_parents_user                 ON parents(user_id);

-- Student-parent mapping
CREATE INDEX idx_spm_student                  ON student_parent_mapping(student_id);
CREATE INDEX idx_spm_parent                   ON student_parent_mapping(parent_id);

-- Student enrollments
CREATE INDEX idx_enrollments_student          ON student_enrollments(student_id, academic_year_id);
CREATE INDEX idx_enrollments_section          ON student_enrollments(section_id, academic_year_id);

-- Timetable
CREATE INDEX idx_timetable_section_day        ON timetable(section_id, day_of_week, effective_from);
CREATE INDEX idx_timetable_exceptions_date    ON timetable_exceptions(section_id, exception_date);

-- Attendance
CREATE INDEX idx_attendance_student_date      ON attendance(student_id, date);
CREATE INDEX idx_attendance_section_date      ON attendance(section_id, date);
CREATE INDEX idx_attendance_school_date       ON attendance(school_id, date);

-- Fee ledger
CREATE INDEX idx_fee_ledger_student           ON student_fee_ledger(student_id, status);
CREATE INDEX idx_fee_ledger_school_year       ON student_fee_ledger(school_id, academic_year_id, status);

-- Fee payments
CREATE INDEX idx_fee_payments_student         ON fee_payments(student_id);
CREATE INDEX idx_fee_payments_school_date     ON fee_payments(school_id, payment_date);

-- Exams
CREATE INDEX idx_exams_grade_year             ON exams(grade_id, academic_year_id);

-- Student marks
CREATE INDEX idx_marks_exam                   ON student_marks(exam_id);
CREATE INDEX idx_marks_student                ON student_marks(student_id);

-- Announcements
CREATE INDEX idx_announcements_school         ON announcements(school_id, published_at DESC);
CREATE INDEX idx_announcements_target         ON announcements(target_type, target_id);

-- Device tokens
CREATE INDEX idx_device_tokens_user           ON device_tokens(user_id, is_active);


-- ============================================================
--  SECTION 12 — SEED DATA
-- ============================================================

-- Seed roles (fixed set — not tenant-specific)
INSERT INTO roles (id, name) VALUES
    (gen_random_uuid(), 'SUPER_ADMIN'),
    (gen_random_uuid(), 'SCHOOL_ADMIN'),
    (gen_random_uuid(), 'PRINCIPAL'),
    (gen_random_uuid(), 'TEACHER'),
    (gen_random_uuid(), 'CLERK'),
    (gen_random_uuid(), 'PARENT');


-- ============================================================
--  END OF SCHEMA
-- ============================================================