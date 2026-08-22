package com.flowset.marketplace.inventory.repository;

import com.flowset.common.ReservationStatus;
import com.flowset.marketplace.inventory.entity.InventoryReservationEntity;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryReservationRepository extends JpaRepository<InventoryReservationEntity, UUID> {

    @Query("""
            select r from InventoryReservationEntity r
            where r.variant.id = :variantId
              and r.status = :status
              and r.startDate <= :endDate
              and r.endDate >= :startDate
            """)
    List<InventoryReservationEntity> findOverlapping(
            @Param("variantId") UUID variantId,
            @Param("status") ReservationStatus status,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    @Query("""
            select r from InventoryReservationEntity r
            where r.variant.id in :variantIds
              and r.status = com.flowset.common.ReservationStatus.CONFIRMED
              and r.startDate <= :endDate
              and r.endDate >= :startDate
            """)
    List<InventoryReservationEntity> findConfirmedOverlappingForVariants(
            @Param("variantIds") Collection<UUID> variantIds,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    List<InventoryReservationEntity> findByVariantIdAndStatusOrderByStartDateAsc(
            UUID variantId, ReservationStatus status);

    @Query("""
            select r from InventoryReservationEntity r
            where r.variant.id = :variantId
              and r.status = com.flowset.common.ReservationStatus.CONFIRMED
              and r.endDate >= :from
            order by r.startDate asc
            """)
    List<InventoryReservationEntity> findUpcomingConfirmed(
            @Param("variantId") UUID variantId, @Param("from") LocalDate from);

    List<InventoryReservationEntity> findByBookingRequestId(UUID bookingRequestId);
}
