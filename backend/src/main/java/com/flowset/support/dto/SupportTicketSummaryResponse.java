package com.flowset.support.dto;

import com.flowset.common.SupportTicketPriority;
import com.flowset.common.SupportTicketStatus;
import java.time.Instant;
import java.util.UUID;

public record SupportTicketSummaryResponse(
        UUID id,
        UUID createdBy,
        String createdByName,
        String subject,
        String categoryName,
        SupportTicketPriority priority,
        SupportTicketStatus status,
        UUID assignedAdminId,
        Instant createdAt,
        Instant updatedAt
) {
}
