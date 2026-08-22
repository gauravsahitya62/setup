package com.flowset.events;

import java.util.UUID;

public record VendorRegisteredEvent(UUID vendorId, UUID ownerUserId, String companyName) {
}
