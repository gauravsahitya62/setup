package com.flowset.orders.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOrderNoteRequest(
        @NotBlank(message = "Note content is required") @Size(max = 4000) String content
) {
}
