package com.flowset.auth.dto;

import com.flowset.shared.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank(message = "Token is required")
        @Size(max = 512, message = "Token is invalid")
        String token,

        @NotBlank(message = "Password is required")
        @StrongPassword
        String newPassword
) {
}
