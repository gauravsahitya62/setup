package com.flowset.planner.projects.dto;

import com.flowset.common.EventType;
import com.flowset.common.ProjectStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

@Schema(description = "Create a planner project. plannerId is taken from the authenticated user.")
public record CreateProjectRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 200, message = "Name must be at most 200 characters")
        String name,
        @NotNull(message = "Event type is required")
        EventType eventType,
        @Size(max = 4000, message = "Description is too long")
        String description,
        LocalDate eventDate,
        LocalTime startTime,
        LocalTime endTime,
        @Size(max = 255) String venueName,
        @Size(max = 500) String venueAddress,
        @Size(max = 120) String city,
        @Size(max = 120) String state,
        @Size(max = 120) String country,
        @Min(value = 0, message = "Guest count cannot be negative")
        Integer guestCount,
        ProjectStatus status
) {
}
