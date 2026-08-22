package com.flowset.planner.projects.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record AddProjectVendorRequest(
        @NotNull(message = "Vendor is required") UUID vendorId,
        @Size(max = 2000) String notes
) {
}
