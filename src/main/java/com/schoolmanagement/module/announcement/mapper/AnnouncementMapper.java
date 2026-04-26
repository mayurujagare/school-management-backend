package com.schoolmanagement.module.announcement.mapper;

import com.schoolmanagement.module.announcement.dto.AnnouncementDto;
import com.schoolmanagement.module.announcement.entity.Announcement;
import org.springframework.stereotype.Component;

@Component
public class AnnouncementMapper {

    public AnnouncementDto.Response toResponse(Announcement a) {
        return AnnouncementDto.Response.builder()
                .id(a.getId())
                .title(a.getTitle())
                .body(a.getBody())
                .targetType(a.getTargetType())
                .targetId(a.getTargetId())
                .isDraft(a.isDraft())
                .publishedAt(a.getPublishedAt())
                .createdAt(a.getCreatedAt())
                .build();
    }

    public AnnouncementDto.Response toResponse(Announcement a,
                                                String targetName,
                                                String createdByName) {
        return AnnouncementDto.Response.builder()
                .id(a.getId())
                .title(a.getTitle())
                .body(a.getBody())
                .targetType(a.getTargetType())
                .targetId(a.getTargetId())
                .targetName(targetName)
                .isDraft(a.isDraft())
                .publishedAt(a.getPublishedAt())
                .createdByName(createdByName)
                .createdAt(a.getCreatedAt())
                .build();
    }
}