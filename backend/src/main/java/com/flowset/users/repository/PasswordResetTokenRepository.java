package com.flowset.users.repository;

import com.flowset.users.entity.PasswordResetTokenEntity;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetTokenEntity, UUID> {

    Optional<PasswordResetTokenEntity> findByTokenHash(String tokenHash);
}
