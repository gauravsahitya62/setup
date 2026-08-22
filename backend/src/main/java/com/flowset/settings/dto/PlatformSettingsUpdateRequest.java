package com.flowset.settings.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record PlatformSettingsUpdateRequest(
        @NotBlank String platformName,
        @NotBlank @Email String supportEmail,
        @Min(0) int defaultInventoryThreshold,
        @Min(1) int reviewEditWindowDays,
        boolean vendorVerificationRequired,
        @Min(1) int maxImageSizeBytes,
        @NotBlank String allowedFileTypes
) {
}
