package com.flowset.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record ReasonRequest(@NotBlank(message = "Reason is required") String reason) {
}
