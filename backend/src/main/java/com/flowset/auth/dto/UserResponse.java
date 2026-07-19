package com.flowset.auth.dto;

import com.flowset.common.RoleName;
import com.flowset.common.UserStatus;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String firstName,
        String lastName,
        String email,
        String phone,
        RoleName role,
        UserStatus status,
        boolean emailVerified,
        String companyName,
        Instant createdAt,
        Instant updatedAt
) {
}
