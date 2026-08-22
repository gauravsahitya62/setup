package com.flowset.events;

import java.util.UUID;

public record VendorVerifiedEvent(UUID vendorId, UUID ownerUserId, String companyName) {
}
