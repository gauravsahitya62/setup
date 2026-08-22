package com.flowset.support.dto;

import com.flowset.common.SupportTicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record CreateSupportTicketRequest(
        @NotBlank String subject,
        @NotBlank String description,
        @NotNull UUID categoryId,
        SupportTicketPriority priority
) {
}
