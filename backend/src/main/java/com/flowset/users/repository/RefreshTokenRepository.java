package com.flowset.users.repository;

import com.flowset.users.entity.RefreshTokenEntity;
import java.util.Optional;
import java.util.UUID;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RefreshTokenEntity r set r.revokedAt = :revokedAt "
            + "where r.user.id = :userId and r.revokedAt is null")
    int revokeAllActiveForUser(@Param("userId") UUID userId, @Param("revokedAt") Instant revokedAt);
}
