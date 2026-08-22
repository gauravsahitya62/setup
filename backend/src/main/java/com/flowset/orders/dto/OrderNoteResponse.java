package com.flowset.orders.dto;

import java.time.Instant;
import java.util.UUID;

public record OrderNoteResponse(UUID id, UUID authorId, String content, Instant createdAt) {
}
