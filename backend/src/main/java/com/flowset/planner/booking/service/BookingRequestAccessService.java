package com.flowset.planner.booking.service;

import com.flowset.common.RoleName;
import com.flowset.exceptions.ForbiddenException;
import com.flowset.exceptions.UnauthorizedException;
import com.flowset.planner.booking.entity.BookingRequestEntity;
import com.flowset.planner.booking.exception.BookingRequestNotFoundException;
import com.flowset.planner.booking.repository.BookingRequestRepository;
import com.flowset.security.UserPrincipal;
import com.flowset.vendor.entity.VendorEntity;
import com.flowset.vendor.repository.VendorRepository;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class BookingRequestAccessService {

    private final BookingRequestRepository bookingRequestRepository;
    private final VendorRepository vendorRepository;

    public BookingRequestAccessService(
            BookingRequestRepository bookingRequestRepository,
            VendorRepository vendorRepository
    ) {
        this.bookingRequestRepository = bookingRequestRepository;
        this.vendorRepository = vendorRepository;
    }

    public UserPrincipal requirePrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new UnauthorizedException("Authentication required");
        }
        return principal;
    }

    public UUID requirePlannerId() {
        UserPrincipal principal = requirePrincipal();
        if (principal.getRole() != RoleName.PLANNER) {
            throw new ForbiddenException("Only planners can create booking requests");
        }
        return principal.getId();
    }

    public VendorEntity requireVendorStorefront() {
        UserPrincipal principal = requirePrincipal();
        if (principal.getRole() != RoleName.VENDOR) {
            throw new ForbiddenException("Only vendors can access vendor booking requests");
        }
        return vendorRepository
                .findActiveByOwnerUserId(principal.getId())
                .orElseThrow(() -> new ForbiddenException("Vendor storefront not found"));
    }

    public BookingRequestEntity requireReadable(UUID id) {
        UserPrincipal principal = requirePrincipal();
        if (principal.getRole() == RoleName.ADMIN) {
            return bookingRequestRepository.findById(id).orElseThrow(BookingRequestNotFoundException::new);
        }
        if (principal.getRole() == RoleName.PLANNER) {
            return bookingRequestRepository
                    .findByIdAndPlannerId(id, principal.getId())
                    .orElseThrow(BookingRequestNotFoundException::new);
        }
        if (principal.getRole() == RoleName.VENDOR) {
            VendorEntity vendor = requireVendorStorefront();
            return bookingRequestRepository
                    .findByIdAndVendorId(id, vendor.getId())
                    .orElseThrow(BookingRequestNotFoundException::new);
        }
        throw new BookingRequestNotFoundException();
    }

    public BookingRequestEntity requirePlannerOwned(UUID id) {
        UserPrincipal principal = requirePrincipal();
        if (principal.getRole() != RoleName.PLANNER) {
            throw new ForbiddenException("Only the requesting planner can cancel this booking");
        }
        return bookingRequestRepository
                .findByIdAndPlannerId(id, principal.getId())
                .orElseThrow(BookingRequestNotFoundException::new);
    }

    public BookingRequestEntity requireVendorOwned(UUID id) {
        VendorEntity vendor = requireVendorStorefront();
        return bookingRequestRepository
                .findByIdAndVendorId(id, vendor.getId())
                .orElseThrow(BookingRequestNotFoundException::new);
    }
}
