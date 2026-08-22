package com.flowset.vendor.dto;

import com.flowset.common.VendorStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record CreateVendorRequest(
        @NotBlank(message = "Company name is required")
        @Size(max = 255, message = "Company name must be at most 255 characters")
        String companyName,

        @Size(max = 255, message = "Slug must be at most 255 characters")
        @Pattern(regexp = "^$|^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug format is invalid")
        String slug,

        @Size(max = 10000, message = "Description is too long")
        String description,

        @Size(max = 500, message = "Short description must be at most 500 characters")
        String shortDescription,

        @Size(max = 50, message = "GST number must be at most 50 characters")
        String gstNumber,

        @Size(max = 100, message = "Business type must be at most 100 characters")
        String businessType,

        @Min(value = 1800, message = "Established year is invalid")
        @Max(value = 2100, message = "Established year is invalid")
        Integer establishedYear,

        @URL(message = "Website must be a valid URL")
        @Size(max = 500)
        String website,

        @Email(message = "Email format is invalid")
        @Size(max = 255)
        String email,

        @Pattern(regexp = "^$|^[+]?[0-9()\\-\\s]{7,20}$", message = "Phone format is invalid")
        String phone,

        VendorStatus status
) {
}
