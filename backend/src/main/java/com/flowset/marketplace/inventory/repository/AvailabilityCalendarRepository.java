package com.flowset.marketplace.inventory.repository;

import com.flowset.marketplace.inventory.entity.AvailabilityCalendarEntity;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AvailabilityCalendarRepository extends JpaRepository<AvailabilityCalendarEntity, UUID> {

    List<AvailabilityCalendarEntity> findByVariantIdAndDateBetweenOrderByDateAsc(
            UUID variantId, LocalDate from, LocalDate to);

    List<AvailabilityCalendarEntity> findByVariantIdOrderByDateAsc(UUID variantId);

    Optional<AvailabilityCalendarEntity> findByVariantIdAndDate(UUID variantId, LocalDate date);
}
