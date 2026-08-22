package com.flowset.orders.mapper;

import com.flowset.common.DeliveryStatus;
import com.flowset.common.OrderStatus;
import com.flowset.common.RoleName;
import com.flowset.marketplace.inventory.engine.DateRange;
import com.flowset.orders.delivery.entity.DeliveryEntity;
import com.flowset.orders.delivery.entity.DeliveryHistoryEntity;
import com.flowset.orders.delivery.entity.DeliveryProofEntity;
import com.flowset.orders.delivery.entity.PickupEntity;
import com.flowset.orders.dto.DeliveryHistoryResponse;
import com.flowset.orders.dto.DeliveryProofResponse;
import com.flowset.orders.dto.DeliveryResponse;
import com.flowset.orders.dto.OrderActions;
import com.flowset.orders.dto.OrderItemResponse;
import com.flowset.orders.dto.OrderNoteResponse;
import com.flowset.orders.dto.OrderResponse;
import com.flowset.orders.dto.OrderSummaryResponse;
import com.flowset.orders.dto.OrderTimelineEventResponse;
import com.flowset.orders.dto.PickupResponse;
import com.flowset.orders.entity.OrderEntity;
import com.flowset.orders.entity.OrderHistoryEntity;
import com.flowset.orders.entity.OrderItemEntity;
import com.flowset.orders.entity.OrderNoteEntity;
import com.flowset.planner.projects.entity.ProjectEntity;
import com.flowset.users.entity.UserEntity;
import com.flowset.vendor.entity.VendorEntity;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class OrderMapper {

    public OrderResponse toResponse(
            OrderEntity order,
            ProjectEntity project,
            UserEntity planner,
            VendorEntity vendor,
            List<OrderItemResponse> items,
            DeliveryResponse delivery,
            PickupResponse pickup,
            List<OrderTimelineEventResponse> timeline,
            List<OrderNoteResponse> notes,
            RoleName role
    ) {
        int rentalDays = DateRange.inclusiveDays(order.getRentalStartDate(), order.getRentalEndDate());
        return new OrderResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getBookingRequestId(),
                order.getProjectId(),
                project == null ? null : project.getName(),
                project == null ? null : project.getEventType(),
                project == null ? null : project.getGuestCount(),
                order.getPlannerId(),
                displayName(planner),
                order.getVendorId(),
                vendor == null ? null : vendor.getCompanyName(),
                vendor == null ? null : vendor.getSlug(),
                vendor == null ? null : vendor.getLogoUrl(),
                order.getStatus(),
                order.getEventDate(),
                order.getRentalStartDate(),
                order.getRentalEndDate(),
                rentalDays,
                order.getDeliveryDate(),
                order.getPickupDate(),
                order.getEstimatedSubtotal(),
                order.getDeliveryFee(),
                order.getSecurityDeposit(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getPlannerNotes(),
                order.getVendorNotes(),
                order.getCreatedAt(),
                order.getUpdatedAt(),
                order.getCompletedAt(),
                order.getCancelledAt(),
                items,
                delivery,
                pickup,
                timeline,
                notes,
                actions(order.getStatus(), delivery, role));
    }

    public OrderSummaryResponse toSummary(
            OrderEntity order,
            ProjectEntity project,
            UserEntity planner,
            VendorEntity vendor,
            List<OrderItemEntity> items
    ) {
        String summary = items.stream()
                .map(item -> item.getQuantity() + " × " + item.getProductNameSnapshot())
                .reduce((a, b) -> a + ", " + b)
                .orElse("No products");
        return new OrderSummaryResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getBookingRequestId(),
                order.getProjectId(),
                project == null ? null : project.getName(),
                order.getVendorId(),
                vendor == null ? null : vendor.getCompanyName(),
                vendor == null ? null : vendor.getLogoUrl(),
                order.getPlannerId(),
                displayName(planner),
                order.getStatus(),
                order.getEventDate(),
                order.getRentalStartDate(),
                order.getRentalEndDate(),
                order.getDeliveryDate(),
                order.getPickupDate(),
                summary,
                items.size(),
                order.getEstimatedSubtotal(),
                order.getDeliveryFee(),
                order.getSecurityDeposit(),
                order.getTotalAmount(),
                order.getCurrency(),
                order.getCreatedAt(),
                order.getUpdatedAt());
    }

    public OrderItemResponse toItem(OrderItemEntity item) {
        return new OrderItemResponse(
                item.getId(),
                item.getProductVariantId(),
                item.getProductNameSnapshot(),
                item.getVariantNameSnapshot(),
                item.getSkuSnapshot(),
                item.getQuantity(),
                item.getPricePerDaySnapshot(),
                item.getRentalDays(),
                item.getSubtotal(),
                item.getCreatedAt());
    }

    public DeliveryResponse toDelivery(DeliveryEntity delivery, List<DeliveryProofEntity> proofs) {
        if (delivery == null) {
            return null;
        }
        return new DeliveryResponse(
                delivery.getId(),
                delivery.getOrder().getId(),
                delivery.getDeliveryDate(),
                delivery.getPreferredTime(),
                delivery.getActualDeliveryTime(),
                delivery.getVenueName(),
                delivery.getVenueAddress(),
                delivery.getCity(),
                delivery.getState(),
                delivery.getContactPerson(),
                delivery.getContactPhone(),
                delivery.getDeliveryNotes(),
                delivery.getStatus(),
                delivery.getCreatedAt(),
                delivery.getUpdatedAt(),
                proofs.stream().map(this::toProof).toList());
    }

    public DeliveryProofResponse toProof(DeliveryProofEntity proof) {
        return new DeliveryProofResponse(
                proof.getId(),
                proof.getFileUrl(),
                proof.getFileName(),
                proof.getFileType(),
                proof.getUploadedAt(),
                proof.getUploadedBy());
    }

    public DeliveryHistoryResponse toDeliveryHistory(DeliveryHistoryEntity history) {
        return new DeliveryHistoryResponse(
                history.getId(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getChangedBy(),
                history.getNote(),
                history.getCreatedAt());
    }

    public PickupResponse toPickup(PickupEntity pickup) {
        if (pickup == null) {
            return null;
        }
        return new PickupResponse(
                pickup.getId(),
                pickup.getOrder().getId(),
                pickup.getPickupDate(),
                pickup.getPreferredTime(),
                pickup.getActualPickupTime(),
                pickup.getNotes(),
                pickup.getStatus(),
                pickup.getCreatedAt(),
                pickup.getUpdatedAt());
    }

    public OrderTimelineEventResponse toTimeline(OrderHistoryEntity history) {
        return new OrderTimelineEventResponse(
                history.getId(),
                history.getEventType(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getDescription(),
                history.getNote(),
                history.getActorId(),
                history.getCreatedAt());
    }

    public OrderNoteResponse toNote(OrderNoteEntity note) {
        return new OrderNoteResponse(note.getId(), note.getAuthorId(), note.getContent(), note.getCreatedAt());
    }

    public OrderActions actions(OrderStatus status, DeliveryResponse delivery, RoleName role) {
        boolean vendor = role == RoleName.VENDOR;
        boolean planner = role == RoleName.PLANNER;
        boolean admin = role == RoleName.ADMIN;
        boolean hasDelivery = delivery != null;
        boolean proofOk = hasDelivery
                && delivery.status() != DeliveryStatus.CANCELLED
                && (status == OrderStatus.OUT_FOR_DELIVERY
                        || status == OrderStatus.DELIVERED
                        || status == OrderStatus.EVENT_COMPLETED);
        return new OrderActions(
                (planner || admin)
                        && (status == OrderStatus.PENDING
                                || status == OrderStatus.CONFIRMED
                                || status == OrderStatus.DELIVERY_SCHEDULED),
                vendor && status == OrderStatus.CONFIRMED,
                vendor && status == OrderStatus.DELIVERY_SCHEDULED,
                vendor && status == OrderStatus.OUT_FOR_DELIVERY,
                vendor && status == OrderStatus.DELIVERED,
                vendor && status == OrderStatus.EVENT_COMPLETED,
                vendor && status == OrderStatus.PICKUP_SCHEDULED,
                (vendor || admin) && proofOk,
                (vendor || admin) && status != OrderStatus.CANCELLED && status != OrderStatus.COMPLETED,
                planner || vendor || admin);
    }

    public static String displayName(UserEntity user) {
        if (user == null) {
            return "Planner";
        }
        return (user.getFirstName() + " " + user.getLastName()).trim();
    }
}
