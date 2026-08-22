package com.flowset.support.dto;

import jakarta.validation.constraints.NotBlank;

public record SupportMessageRequest(@NotBlank String body) {
}
