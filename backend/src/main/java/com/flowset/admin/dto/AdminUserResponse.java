package com.flowset.admin.dto;

import com.flowset.common.RoleName;
import com.flowset.common.UserStatus;
import java.time.Instant;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phone,
        RoleName role,
        UserStatus status,
        boolean emailVerified,
        Instant createdAt,
        Instant lastLoginAt,
        Instant deletedAt
) {
}
