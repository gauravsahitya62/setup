package com.flowset.planner.booking.dto;

import java.time.Instant;
import java.util.UUID;

public record BookingNoteResponse(UUID id, UUID authorId, String content, Instant createdAt) {
}
