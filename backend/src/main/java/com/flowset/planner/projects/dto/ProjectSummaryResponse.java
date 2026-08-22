package com.flowset.planner.projects.dto;

import com.flowset.common.EventType;
import com.flowset.common.ProjectStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ProjectSummaryResponse(
        UUID id,
        UUID plannerId,
        String name,
        String slug,
        EventType eventType,
        String description,
        LocalDate eventDate,
        String venueName,
        String city,
        Integer guestCount,
        ProjectStatus status,
        String coverImageUrl,
        long productCount,
        long vendorCount,
        int progressPercent,
        Instant createdAt,
        Instant updatedAt
) {
}
