package com.flowset.vendor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ServiceAreaRequest(
        @NotBlank(message = "Country is required")
        @Size(max = 100)
        String country,

        @NotBlank(message = "State is required")
        @Size(max = 100)
        String state,

        @NotBlank(message = "City is required")
        @Size(max = 100)
        String city
) {
}
