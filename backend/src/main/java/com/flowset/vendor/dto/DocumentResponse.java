package com.flowset.vendor.dto;

import com.flowset.common.DocumentVerificationStatus;
import com.flowset.common.VendorDocumentType;
import java.time.Instant;
import java.util.UUID;

public record DocumentResponse(
        UUID id,
        String fileUrl,
        String fileName,
        VendorDocumentType documentType,
        DocumentVerificationStatus verificationStatus,
        String reviewReason,
        Instant reviewedAt
) {
}
