package com.flowset.settings.repository;

import com.flowset.settings.entity.PlatformSettingsEntity;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlatformSettingsRepository extends JpaRepository<PlatformSettingsEntity, UUID> {
}
