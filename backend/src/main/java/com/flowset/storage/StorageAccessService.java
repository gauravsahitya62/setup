package com.flowset.storage;

import com.flowset.common.RoleName;
import com.flowset.exceptions.ForbiddenException;
import com.flowset.exceptions.UnauthorizedException;
import com.flowset.orders.repository.OrderRepository;
import com.flowset.planner.projects.repository.ProjectRepository;
import com.flowset.security.UserPrincipal;
import com.flowset.vendor.repository.VendorRepository;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class StorageAccessService {

    private final VendorRepository vendorRepository;
    private final ProjectRepository projectRepository;
    private final OrderRepository orderRepository;

    public StorageAccessService(
            VendorRepository vendorRepository,
            ProjectRepository projectRepository,
            OrderRepository orderRepository
    ) {
        this.vendorRepository = vendorRepository;
        this.projectRepository = projectRepository;
        this.orderRepository = orderRepository;
    }

    public void assertCanRead(String relativePath) {
        if (StorageVisibility.isPublic(relativePath)) {
            return;
        }
        UserPrincipal principal = currentPrincipal();
        if (principal == null) {
            throw new UnauthorizedException("Authentication required");
        }
        if (principal.getRole() == RoleName.ADMIN) {
            return;
        }

        String vendorId = StorageVisibility.vendorIdFromDocumentPath(relativePath);
        if (vendorId != null) {
            boolean owner = vendorRepository
                    .findActiveById(parseUuid(vendorId))
                    .map(vendor -> vendor.getOwnerUserId().equals(principal.getId()))
                    .orElse(false);
            if (!owner) {
                throw new ForbiddenException("Access denied");
            }
            return;
        }

        String projectId = StorageVisibility.projectIdFromPath(relativePath);
        if (projectId != null) {
            boolean owner = projectRepository
                    .findActiveByIdAndPlannerId(parseUuid(projectId), principal.getId())
                    .isPresent();
            if (!owner) {
                throw new ForbiddenException("Access denied");
            }
            return;
        }

        String orderId = StorageVisibility.orderIdFromPath(relativePath);
        if (orderId != null) {
            UUID id = parseUuid(orderId);
            if (orderRepository.findByIdAndPlannerId(id, principal.getId()).isPresent()) {
                return;
            }
            boolean vendorOwner = vendorRepository
                    .findActiveByOwnerUserId(principal.getId())
                    .map(vendor -> orderRepository.findByIdAndVendorId(id, vendor.getId()).isPresent())
                    .orElse(false);
            if (!vendorOwner) {
                throw new ForbiddenException("Access denied");
            }
            return;
        }

        throw new ForbiddenException("Access denied");
    }

    private static UUID parseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException e) {
            throw new ForbiddenException("Access denied");
        }
    }

    private static UserPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }
        return null;
    }
}
