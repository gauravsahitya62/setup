package com.flowset.orders.validation;

import com.flowset.exceptions.BadRequestException;
import java.time.LocalDate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Component
public class OrderValidator {

    public static final long MAX_PROOF_BYTES = 10L * 1024 * 1024;

    public void requireDeliveryContact(String venueName, String venueAddress, String contactPerson, String contactPhone) {
        if (!StringUtils.hasText(venueName) && !StringUtils.hasText(venueAddress)) {
            throw new BadRequestException("Venue is required to schedule delivery");
        }
        if (!StringUtils.hasText(contactPerson) || !StringUtils.hasText(contactPhone)) {
            throw new BadRequestException("Contact person and phone are required to schedule delivery");
        }
    }

    public void requirePickupAfterDelivery(LocalDate deliveryDate, LocalDate pickupDate) {
        if (deliveryDate == null) {
            throw new BadRequestException("Delivery must be scheduled before pickup");
        }
        if (pickupDate == null) {
            throw new BadRequestException("Pickup date is required");
        }
        if (pickupDate.isBefore(deliveryDate)) {
            throw new BadRequestException("Pickup cannot be scheduled before delivery");
        }
    }

    public void requireDeliveryDate(LocalDate deliveryDate, LocalDate rentalStart, LocalDate rentalEnd) {
        if (deliveryDate == null) {
            throw new BadRequestException("Delivery date is required");
        }
        if (rentalEnd != null && deliveryDate.isAfter(rentalEnd)) {
            throw new BadRequestException("Delivery date cannot be after the rental end date");
        }
        if (rentalStart != null && deliveryDate.isBefore(rentalStart.minusDays(2))) {
            throw new BadRequestException("Delivery date is too far before the rental start date");
        }
    }

    public void requireProofFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Proof file is required");
        }
        if (file.getSize() > MAX_PROOF_BYTES) {
            throw new BadRequestException("Proof file must be 10 MB or smaller");
        }
    }
}
