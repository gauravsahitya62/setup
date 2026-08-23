package com.flowset.orders.repository;

import com.flowset.common.OrderStatus;
import com.flowset.orders.entity.OrderEntity;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface OrderRepository extends JpaRepository<OrderEntity, UUID>, JpaSpecificationExecutor<OrderEntity> {

    Optional<OrderEntity> findByBookingRequestId(UUID bookingRequestId);

    List<OrderEntity> findByBookingRequestIdIn(Collection<UUID> bookingRequestIds);

    Optional<OrderEntity> findByIdAndPlannerId(UUID id, UUID plannerId);

    Optional<OrderEntity> findByIdAndVendorId(UUID id, UUID vendorId);

    List<OrderEntity> findByVendorIdAndStatusIn(UUID vendorId, Collection<OrderStatus> statuses);

    List<OrderEntity> findByProjectIdOrderByCreatedAtDesc(UUID projectId);

    List<OrderEntity> findByPlannerIdAndStatusOrderByCompletedAtDesc(UUID plannerId, OrderStatus status);

    boolean existsByBookingRequestId(UUID bookingRequestId);

    long countByStatus(OrderStatus status);

    long countByCreatedAtGreaterThanEqual(java.time.Instant from);

    long countByStatusAndCreatedAtGreaterThanEqual(OrderStatus status, java.time.Instant from);

    long countByVendorIdAndStatus(UUID vendorId, OrderStatus status);

    java.util.List<OrderEntity> findByCreatedAtGreaterThanEqualOrderByCreatedAtAsc(java.time.Instant from);

    java.util.List<OrderEntity> findByVendorIdOrderByCreatedAtDesc(UUID vendorId);

    @org.springframework.data.jpa.repository.Query(
            "select coalesce(sum(o.totalAmount), 0) from OrderEntity o where o.status <> com.flowset.common.OrderStatus.CANCELLED")
    java.math.BigDecimal sumEstimatedRentalValue();

    @org.springframework.data.jpa.repository.Query(
            "select coalesce(sum(o.totalAmount), 0) from OrderEntity o where o.vendorId = :vendorId and o.status <> com.flowset.common.OrderStatus.CANCELLED")
    java.math.BigDecimal sumEstimatedRentalValueByVendor(
            @org.springframework.data.repository.query.Param("vendorId") UUID vendorId);
}
