package com.flowset.reviews.service;

import com.flowset.common.RoleName;
import com.flowset.exceptions.ForbiddenException;
import com.flowset.exceptions.UnauthorizedException;
import com.flowset.reviews.entity.ReviewEntity;
import com.flowset.reviews.exception.ReviewNotFoundException;
import com.flowset.reviews.repository.ReviewRepository;
import com.flowset.security.UserPrincipal;
import com.flowset.vendor.entity.VendorEntity;
import com.flowset.vendor.repository.VendorRepository;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class ReviewAccessService {

    private final ReviewRepository reviewRepository;
    private final VendorRepository vendorRepository;

    public ReviewAccessService(ReviewRepository reviewRepository, VendorRepository vendorRepository) {
        this.reviewRepository = reviewRepository;
        this.vendorRepository = vendorRepository;
    }

    public UserPrincipal requirePrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new UnauthorizedException("Authentication required");
        }
        return principal;
    }

    public UserPrincipal requirePlanner() {
        UserPrincipal principal = requirePrincipal();
        if (principal.getRole() != RoleName.PLANNER) {
            throw new ForbiddenException("Only planners can submit reviews");
        }
        return principal;
    }

    public UserPrincipal requireAdmin() {
        UserPrincipal principal = requirePrincipal();
        if (principal.getRole() != RoleName.ADMIN) {
            throw new ForbiddenException("Admin access required");
        }
        return principal;
    }

    public VendorEntity requireVendorStorefront() {
        UserPrincipal principal = requirePrincipal();
        if (principal.getRole() != RoleName.VENDOR) {
            throw new ForbiddenException("Only vendors can access vendor reviews");
        }
        return vendorRepository
                .findActiveByOwnerUserId(principal.getId())
                .orElseThrow(() -> new ForbiddenException("Vendor storefront not found"));
    }

    public ReviewEntity requireReadable(UUID id) {
        UserPrincipal principal = requirePrincipal();
        if (principal.getRole() == RoleName.ADMIN) {
            return reviewRepository.findById(id).orElseThrow(ReviewNotFoundException::new);
        }
        if (principal.getRole() == RoleName.PLANNER) {
            return reviewRepository.findByIdAndPlannerId(id, principal.getId()).orElseThrow(ReviewNotFoundException::new);
        }
        if (principal.getRole() == RoleName.VENDOR) {
            VendorEntity vendor = requireVendorStorefront();
            ReviewEntity review = reviewRepository.findById(id).orElseThrow(ReviewNotFoundException::new);
            if (!vendor.getId().equals(review.getVendorId())
                    || review.getDeletedAt() != null
                    || review.getStatus() != com.flowset.common.ReviewStatus.PUBLISHED) {
                throw new ReviewNotFoundException();
            }
            return review;
        }
        throw new ReviewNotFoundException();
    }

    public ReviewEntity requirePlannerOwned(UUID id) {
        UserPrincipal principal = requirePlanner();
        return reviewRepository.findByIdAndPlannerId(id, principal.getId()).orElseThrow(ReviewNotFoundException::new);
    }
}
