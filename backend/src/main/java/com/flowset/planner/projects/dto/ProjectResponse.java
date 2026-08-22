package com.flowset.planner.projects.dto;

import com.flowset.common.EventType;
import com.flowset.common.ProjectStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Schema(description = "Project workspace summary including setup progress")
public record ProjectResponse(
        UUID id,
        UUID plannerId,
        String name,
        String slug,
        EventType eventType,
        String description,
        LocalDate eventDate,
        LocalTime startTime,
        LocalTime endTime,
        String venueName,
        String venueAddress,
        String city,
        String state,
        String country,
        Integer guestCount,
        ProjectStatus status,
        String coverImageUrl,
        long productCount,
        long vendorCount,
        long moodBoardItemCount,
        long noteCount,
        long fileCount,
        ProjectProgressResponse progress,
        List<ProjectActivityResponse> recentActivity,
        Instant createdAt,
        Instant updatedAt
) {
}
