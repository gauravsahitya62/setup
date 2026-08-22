package com.flowset.vendor.dto;

import java.util.UUID;

public record ServiceAreaResponse(UUID id, String country, String state, String city) {
}
