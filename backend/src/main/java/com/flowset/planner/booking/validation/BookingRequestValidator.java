package com.flowset.planner.booking.validation;

import com.flowset.common.ProductStatus;
import com.flowset.common.VariantStatus;
import com.flowset.common.VendorStatus;
import com.flowset.exceptions.BadRequestException;
import com.flowset.marketplace.inventory.engine.DateRange;
import com.flowset.marketplace.product.entity.ProductEntity;
import com.flowset.marketplace.product.entity.ProductVariantEntity;
import com.flowset.planner.booking.dto.CreateBookingRequest;
import com.flowset.vendor.entity.VendorEntity;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class BookingRequestValidator {

    public void requireVendorActive(VendorEntity vendor) {
        if (vendor == null || vendor.isDeleted() || vendor.getStatus() == VendorStatus.SUSPENDED) {
            throw new BadRequestException("Vendor is not active");
        }
        if (vendor.getStatus() != VendorStatus.PUBLISHED) {
            throw new BadRequestException("Vendor is not active");
        }
    }

    public void requireDates(LocalDate eventDate, LocalDate rentalStart, LocalDate rentalEnd) {
        if (eventDate == null) {
            throw new BadRequestException("Event date is required");
        }
        DateRange.requireValid(rentalStart, rentalEnd);
        if (eventDate.isBefore(rentalStart) || eventDate.isAfter(rentalEnd)) {
            throw new BadRequestException("Event date must fall within the rental period");
        }
    }

    public void requireUniqueVariants(CreateBookingRequest request) {
        Set<UUID> seen = new HashSet<>();
        for (CreateBookingRequest.Item item : request.items()) {
            if (!seen.add(item.variantId())) {
                throw new BadRequestException("Each product variant can appear only once on a booking request");
            }
        }
    }

    public void requireLiveVariant(ProductVariantEntity variant, UUID vendorId) {
        if (variant == null) {
            throw new BadRequestException("Product variant is no longer available");
        }
        if (variant.getStatus() != VariantStatus.ACTIVE) {
            throw new BadRequestException("Product variant is no longer available");
        }
        ProductEntity product = variant.getProduct();
        if (product == null || product.isDeleted() || product.getStatus() == ProductStatus.ARCHIVED
                || product.getStatus() == ProductStatus.SUSPENDED
                || product.getStatus() == ProductStatus.REJECTED
                || product.getStatus() == ProductStatus.DRAFT
                || product.getStatus() == ProductStatus.PENDING_REVIEW) {
            throw new BadRequestException("Product variant is no longer available");
        }
        if (product.getVendor() == null || !product.getVendor().getId().equals(vendorId)) {
            throw new BadRequestException("Product does not belong to the selected vendor");
        }
    }
}
