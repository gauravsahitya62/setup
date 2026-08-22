package com.flowset.support.dto;

import java.time.Instant;
import java.util.UUID;

public record SupportMessageResponse(UUID id, UUID authorId, String authorName, String body, Instant createdAt) {
}
