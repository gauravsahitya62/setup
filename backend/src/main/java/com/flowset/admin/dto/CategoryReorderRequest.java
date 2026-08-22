package com.flowset.admin.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record CategoryReorderRequest(@NotEmpty List<UUID> orderedIds) {
}
