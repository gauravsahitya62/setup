package com.flowset.support.dto;

import com.flowset.common.SupportTicketPriority;
import com.flowset.common.SupportTicketStatus;
import java.util.UUID;

public record AdminSupportTicketUpdateRequest(
        SupportTicketStatus status,
        SupportTicketPriority priority,
        UUID assignedAdminId
) {
}
