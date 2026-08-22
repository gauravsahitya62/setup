package com.flowset.support.dto;

import java.util.UUID;

public record SupportCategoryResponse(UUID id, String name, String slug, String description, int displayOrder) {
}
