package com.flowset.auth.dto;

import com.flowset.shared.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;

public record ResetPasswordRequest(
        @NotBlank(message = "Token is required")
        String token,

        @NotBlank(message = "Password is required")
        @StrongPassword
        String newPassword
) {
}
