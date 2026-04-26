package com.schoolmanagement.module.announcement.service;

import com.schoolmanagement.common.exception.AppException;
import com.schoolmanagement.common.exception.ErrorCode;
import com.schoolmanagement.common.tenant.TenantContext;
import com.schoolmanagement.module.academic.entity.Grade;
import com.schoolmanagement.module.academic.entity.Section;
import com.schoolmanagement.module.academic.repository.GradeRepository;
import com.schoolmanagement.module.academic.repository.SectionRepository;
import com.schoolmanagement.module.announcement.dto.AnnouncementDto;
import com.schoolmanagement.module.announcement.entity.Announcement;
import com.schoolmanagement.module.announcement.entity.Announcement.TargetType;
import com.schoolmanagement.module.announcement.mapper.AnnouncementMapper;
import com.schoolmanagement.module.announcement.repository.AnnouncementRepository;
import com.schoolmanagement.module.auth.entity.User;
import com.schoolmanagement.module.auth.repository.UserRepository;
import com.schoolmanagement.module.student.entity.StudentEnrollment;
import com.schoolmanagement.module.student.entity.StudentParentMapping;
import com.schoolmanagement.module.student.repository.StudentEnrollmentRepository;
import com.schoolmanagement.module.student.repository.StudentParentMappingRepository;
import com.schoolmanagement.module.school.service.AcademicYearService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository          announcementRepository;
    private final GradeRepository                 gradeRepository;
    private final SectionRepository               sectionRepository;
    private final UserRepository                  userRepository;
    private final StudentParentMappingRepository  parentMappingRepository;
    private final StudentEnrollmentRepository     enrollmentRepository;
    private final AcademicYearService             academicYearService;
    private final PushNotificationService         pushNotificationService;
    private final AnnouncementMapper              mapper;

    // ── Create announcement ───────────────────────────────────
    @Transactional
    public AnnouncementDto.Response create(AnnouncementDto.CreateRequest request) {
        UUID schoolId  = TenantContext.getTenantId();
        UUID createdBy = getCurrentUserId();

        // Validate target
        validateTarget(request.getTargetType(), request.getTargetId(), schoolId);

        Announcement announcement = Announcement.builder()
                .schoolId(schoolId)
                .title(request.getTitle().trim())
                .body(request.getBody().trim())
                .targetType(request.getTargetType())
                .targetId(request.getTargetId())
                .createdBy(createdBy)
                .build();

        // Publish immediately if requested
        if (Boolean.TRUE.equals(request.getPublishNow())) {
            announcement.setPublishedAt(LocalDateTime.now());
        }

        Announcement saved = announcementRepository.save(announcement);

        log.info("Announcement created: [{}] target: {} draft: {}",
                saved.getTitle(), saved.getTargetType(), saved.isDraft());

        // Send push notifications if published
        if (saved.isPublished()) {
            pushNotificationService.sendPushForAnnouncement(saved);
        }

        return buildResponse(saved);
    }

    // ── Update announcement (draft only) ──────────────────────
    @Transactional
    public AnnouncementDto.Response update(UUID announcementId,
                                            AnnouncementDto.UpdateRequest request) {
        UUID schoolId = TenantContext.getTenantId();

        Announcement announcement = announcementRepository
                .findByIdAndSchoolId(announcementId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Announcement not found"));

        if (announcement.isPublished()) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Cannot edit a published announcement");
        }

        if (request.getTitle()      != null)
            announcement.setTitle(request.getTitle().trim());
        if (request.getBody()       != null)
            announcement.setBody(request.getBody().trim());
        if (request.getTargetType() != null) {
            validateTarget(request.getTargetType(), request.getTargetId(), schoolId);
            announcement.setTargetType(request.getTargetType());
            announcement.setTargetId(request.getTargetId());
        }

        return buildResponse(announcementRepository.save(announcement));
    }

    // ── Publish a draft ───────────────────────────────────────
    @Transactional
    public AnnouncementDto.Response publish(UUID announcementId) {
        UUID schoolId = TenantContext.getTenantId();

        Announcement announcement = announcementRepository
                .findByIdAndSchoolId(announcementId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Announcement not found"));

        if (announcement.isPublished()) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Announcement is already published");
        }

        announcement.setPublishedAt(LocalDateTime.now());
        Announcement saved = announcementRepository.save(announcement);

        log.info("Announcement published: [{}]", saved.getTitle());

        // Send push notifications
        pushNotificationService.sendPushForAnnouncement(saved);

        return buildResponse(saved);
    }

    // ── Delete draft ──────────────────────────────────────────
    @Transactional
    public void deleteDraft(UUID announcementId) {
        UUID schoolId = TenantContext.getTenantId();

        Announcement announcement = announcementRepository
                .findByIdAndSchoolId(announcementId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Announcement not found"));

        if (announcement.isPublished()) {
            throw new AppException(ErrorCode.INVALID_REQUEST,
                    "Cannot delete a published announcement");
        }

        announcementRepository.delete(announcement);
        log.info("Draft announcement deleted: [{}]", announcement.getTitle());
    }

    // ── Get by ID ─────────────────────────────────────────────
    public AnnouncementDto.Response getById(UUID announcementId) {
        UUID schoolId = TenantContext.getTenantId();

        Announcement announcement = announcementRepository
                .findByIdAndSchoolId(announcementId, schoolId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                        "Announcement not found"));

        return buildResponse(announcement);
    }

    // ── List all (admin view) ─────────────────────────────────
    public Page<AnnouncementDto.Response> listAll(TargetType targetType,
                                                    Boolean isDraft,
                                                    Pageable pageable) {
        UUID schoolId = TenantContext.getTenantId();

        return announcementRepository
                .findBySchoolWithFilters(schoolId, targetType, isDraft, pageable)
                .map(this::buildResponse);
    }

    // ── Parent feed — announcements relevant to parent ────────
    public Page<AnnouncementDto.Response> getParentFeed(UUID userId,
                                                          Pageable pageable) {
        UUID schoolId = TenantContext.getTenantId();

        // Find all students linked to this parent
        List<StudentParentMapping> mappings = parentMappingRepository
                .findStudentsByParentId(getParentIdFromUserId(userId));

        if (mappings.isEmpty()) {
            return announcementRepository
                    .findAllPublished(schoolId, pageable)
                    .map(this::buildResponse);
        }

        // Get current academic year
        UUID yearId = academicYearService.getCurrentYearEntity(schoolId).getId();

        // Collect all grade IDs and section IDs from children's enrollments
        List<UUID> gradeIds   = new ArrayList<>();
        List<UUID> sectionIds = new ArrayList<>();

        for (StudentParentMapping m : mappings) {
            enrollmentRepository
                    .findByStudent_IdAndAcademicYear_Id(
                            m.getStudent().getId(), yearId)
                    .ifPresent(enrollment -> {
                        sectionIds.add(enrollment.getSection().getId());
                        gradeIds.add(enrollment.getSection().getGrade().getId());
                    });
        }

        // If no enrollments found, show ALL announcements only
        if (gradeIds.isEmpty() && sectionIds.isEmpty()) {
            gradeIds.add(UUID.randomUUID());    // dummy to avoid empty IN clause
            sectionIds.add(UUID.randomUUID());
        }

        return announcementRepository
                .findPublishedForParent(schoolId, gradeIds, sectionIds, pageable)
                .map(this::buildResponse);
    }

    // ── Stats for dashboard ───────────────────────────────────
    public AnnouncementDto.Stats getStats() {
        UUID schoolId = TenantContext.getTenantId();
        long total  = announcementRepository.count();
        long drafts = announcementRepository
                .countBySchoolIdAndPublishedAtIsNull(schoolId);

        return AnnouncementDto.Stats.builder()
                .totalAnnouncements(total)
                .drafts(drafts)
                .published(total - drafts)
                .build();
    }

    // ── Private: validate target ──────────────────────────────
    private void validateTarget(TargetType targetType, UUID targetId,
                                 UUID schoolId) {
        if (targetType == TargetType.GRADE) {
            if (targetId == null) {
                throw new AppException(ErrorCode.INVALID_REQUEST,
                        "Grade ID is required for GRADE target type");
            }
            gradeRepository.findByIdAndSchoolId(targetId, schoolId)
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                            "Grade not found"));
        }

        if (targetType == TargetType.SECTION) {
            if (targetId == null) {
                throw new AppException(ErrorCode.INVALID_REQUEST,
                        "Section ID is required for SECTION target type");
            }
            sectionRepository.findByIdAndSchoolId(targetId, schoolId)
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND,
                            "Section not found"));
        }
    }

    // ── Private: build response with resolved names ───────────
    private AnnouncementDto.Response buildResponse(Announcement a) {
        String targetName  = resolveTargetName(a);
        String createdName = resolveCreatedByName(a.getCreatedBy());
        return mapper.toResponse(a, targetName, createdName);
    }

    private String resolveTargetName(Announcement a) {
        if (a.getTargetId() == null) return null;

        return switch (a.getTargetType()) {
            case ALL     -> "Entire School";
            case GRADE   -> gradeRepository.findById(a.getTargetId())
                    .map(Grade::getName).orElse("Unknown Grade");
            case SECTION -> sectionRepository.findById(a.getTargetId())
                    .map(s -> s.getGrade().getName() + " - " + s.getName())
                    .orElse("Unknown Section");
        };
    }

    private String resolveCreatedByName(UUID userId) {
        if (userId == null) return null;
        return userRepository.findById(userId)
                .map(User::getFullName)
                .orElse("Unknown");
    }

    private UUID getParentIdFromUserId(UUID userId) {
        // Parent ID lookup — the parent entity is linked to user
        // We use the mapping repository which joins through parent → user
        // For simplicity, we search mappings by iterating
        // In production you'd add a dedicated query
        return userId; // parentMappingRepository uses parent.user.id internally
    }

    private UUID getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UUID)
            return (UUID) auth.getPrincipal();
        return null;
    }
}