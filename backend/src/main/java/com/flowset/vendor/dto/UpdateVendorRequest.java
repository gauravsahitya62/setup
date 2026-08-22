package com.flowset.vendor.dto;

import com.flowset.common.VendorStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.URL;

public record UpdateVendorRequest(
        @NotBlank(message = "Company name is required")
        @Size(max = 255)
        String companyName,

        @Size(max = 255)
        @Pattern(regexp = "^$|^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug format is invalid")
        String slug,

        @Size(max = 10000)
        String description,

        @Size(max = 500)
        String shortDescription,

        @Size(max = 50)
        String gstNumber,

        @Size(max = 100)
        String businessType,

        @Min(1800)
        @Max(2100)
        Integer establishedYear,

        @URL(message = "Website must be a valid URL")
        @Size(max = 500)
        String website,

        @Email(message = "Email format is invalid")
        @Size(max = 255)
        String email,

        @Pattern(regexp = "^$|^[+]?[0-9()\\-\\s]{7,20}$", message = "Phone format is invalid")
        String phone,

        Integer responseTimeMinutes,

        Integer completedEvents,

        VendorStatus status
) {
}
