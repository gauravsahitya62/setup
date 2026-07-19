package com.flowset.auth.dto;

import com.flowset.shared.validation.StrongPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterVendorRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must be at most 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must be at most 100 characters")
        String lastName,

        @NotBlank(message = "Email is required")
        @Email(message = "Email format is invalid")
        @Size(max = 255, message = "Email must be at most 255 characters")
        String email,

        @Pattern(
                regexp = "^$|^[+]?[0-9()\\-\\s]{7,20}$",
                message = "Phone format is invalid"
        )
        String phone,

        @NotBlank(message = "Password is required")
        @StrongPassword
        String password,

        @NotBlank(message = "Business name is required")
        @Size(max = 255, message = "Business name must be at most 255 characters")
        String businessName
) {
}
