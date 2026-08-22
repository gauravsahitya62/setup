package com.flowset.planner.booking.mapper;

import com.flowset.common.AvailabilityLevel;
import com.flowset.marketplace.inventory.engine.DateRange;
import com.flowset.planner.booking.dto.BookingDeliveryResponse;
import com.flowset.planner.booking.dto.BookingNoteResponse;
import com.flowset.planner.booking.dto.BookingRequestItemResponse;
import com.flowset.planner.booking.dto.BookingRequestResponse;
import com.flowset.planner.booking.dto.BookingRequestSummaryResponse;
import com.flowset.planner.booking.dto.BookingTimelineEventResponse;
import com.flowset.planner.booking.entity.BookingRequestDeliveryEntity;
import com.flowset.planner.booking.entity.BookingRequestEntity;
import com.flowset.planner.booking.entity.BookingRequestHistoryEntity;
import com.flowset.planner.booking.entity.BookingRequestItemEntity;
import com.flowset.planner.booking.entity.BookingRequestNoteEntity;
import com.flowset.planner.booking.service.BookingPricing;
import com.flowset.planner.projects.entity.ProjectEntity;
import com.flowset.users.entity.UserEntity;
import com.flowset.vendor.entity.VendorEntity;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class BookingRequestMapper {

    public BookingRequestResponse toResponse(
            BookingRequestEntity request,
            ProjectEntity project,
            UserEntity planner,
            VendorEntity vendor,
            List<BookingRequestItemResponse> items,
            BookingDeliveryResponse delivery,
            List<BookingTimelineEventResponse> timeline,
            List<BookingNoteResponse> notes,
            UUID orderId
    ) {
        int rentalDays = DateRange.inclusiveDays(request.getRentalStartDate(), request.getRentalEndDate());
        return new BookingRequestResponse(
                request.getId(),
                request.getProjectId(),
                project == null ? null : project.getName(),
                project == null ? null : project.getEventType(),
                project == null ? null : project.getGuestCount(),
                request.getPlannerId(),
                displayName(planner),
                request.getVendorId(),
                vendor == null ? null : vendor.getCompanyName(),
                vendor == null ? null : vendor.getSlug(),
                vendor == null ? null : vendor.getLogoUrl(),
                vendor != null && vendor.isVerified(),
                request.getStatus(),
                request.getEventDate(),
                request.getRentalStartDate(),
                request.getRentalEndDate(),
                rentalDays,
                request.getDeliveryDate(),
                request.getPickupDate(),
                request.getVenueName(),
                request.getVenueAddress(),
                request.getCity(),
                request.getState(),
                request.getPlannerNotes(),
                request.getVendorNotes(),
                request.getRejectionReason(),
                request.getEstimatedRentalValue(),
                request.getCurrency(),
                request.getRequestedAt(),
                request.getRespondedAt(),
                request.getViewedAt(),
                request.getCancelledAt(),
                request.getCreatedAt(),
                request.getUpdatedAt(),
                orderId,
                items,
                delivery,
                timeline,
                notes);
    }

    public BookingRequestSummaryResponse toSummary(
            BookingRequestEntity request,
            ProjectEntity project,
            UserEntity planner,
            VendorEntity vendor,
            List<BookingRequestItemEntity> items
    ) {
        String summary = items.stream()
                .map(item -> item.getQuantity() + " × " + item.getProductNameSnapshot())
                .reduce((a, b) -> a + ", " + b)
                .orElse("No products");
        return new BookingRequestSummaryResponse(
                request.getId(),
                request.getProjectId(),
                project == null ? null : project.getName(),
                request.getVendorId(),
                vendor == null ? null : vendor.getCompanyName(),
                vendor == null ? null : vendor.getLogoUrl(),
                request.getPlannerId(),
                displayName(planner),
                request.getStatus(),
                request.getEventDate(),
                request.getRentalStartDate(),
                request.getRentalEndDate(),
                summary,
                items.size(),
                request.getEstimatedRentalValue(),
                request.getCurrency(),
                request.getRequestedAt(),
                request.getCreatedAt(),
                request.getUpdatedAt());
    }

    public BookingRequestItemResponse toItem(
            BookingRequestItemEntity item,
            AvailabilityLevel availabilityStatus,
            Integer availableQuantity,
            String availabilityMessage
    ) {
        int days = DateRange.inclusiveDays(item.getRentalStartDate(), item.getRentalEndDate());
        BigDecimal line = BookingPricing.lineValue(item.getPricePerDaySnapshot(), item.getQuantity(), days);
        return new BookingRequestItemResponse(
                item.getId(),
                item.getProductVariantId(),
                item.getProductNameSnapshot(),
                item.getVariantNameSnapshot(),
                item.getSkuSnapshot(),
                item.getVendorId(),
                item.getQuantity(),
                item.getPricePerDaySnapshot(),
                item.getRentalStartDate(),
                item.getRentalEndDate(),
                days,
                line,
                item.getNotes(),
                availabilityStatus,
                availableQuantity,
                availabilityMessage);
    }

    public BookingDeliveryResponse toDelivery(BookingRequestDeliveryEntity delivery) {
        if (delivery == null) {
            return null;
        }
        return new BookingDeliveryResponse(
                delivery.getDeliveryDate(),
                delivery.getPreferredDeliveryTime(),
                delivery.getVenueName(),
                delivery.getVenueAddress(),
                delivery.getCity(),
                delivery.getState(),
                delivery.getContactPerson(),
                delivery.getContactPhone(),
                delivery.getDeliveryNotes(),
                delivery.getPickupDate(),
                delivery.getPreferredPickupTime());
    }

    public BookingTimelineEventResponse toTimeline(BookingRequestHistoryEntity history) {
        return new BookingTimelineEventResponse(
                history.getId(),
                history.getEventType(),
                history.getFromStatus(),
                history.getToStatus(),
                history.getDescription(),
                history.getNote(),
                history.getActorId(),
                history.getCreatedAt());
    }

    public BookingNoteResponse toNote(BookingRequestNoteEntity note) {
        return new BookingNoteResponse(note.getId(), note.getAuthorId(), note.getContent(), note.getCreatedAt());
    }

    public static String displayName(UserEntity user) {
        if (user == null) {
            return "Planner";
        }
        return (user.getFirstName() + " " + user.getLastName()).trim();
    }
}
