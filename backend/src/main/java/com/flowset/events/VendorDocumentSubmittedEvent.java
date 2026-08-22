package com.flowset.events;

import com.flowset.common.VendorDocumentType;
import java.util.UUID;

public record VendorDocumentSubmittedEvent(
        UUID vendorId,
        UUID ownerUserId,
        String companyName,
        VendorDocumentType documentType
) {
}
