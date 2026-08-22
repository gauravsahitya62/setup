package com.flowset.orders.service;

import com.flowset.common.RoleName;
import com.flowset.exceptions.ForbiddenException;
import com.flowset.exceptions.UnauthorizedException;
import com.flowset.orders.entity.OrderEntity;
import com.flowset.orders.exception.OrderNotFoundException;
import com.flowset.orders.repository.OrderRepository;
import com.flowset.security.UserPrincipal;
import com.flowset.vendor.entity.VendorEntity;
import com.flowset.vendor.repository.VendorRepository;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class OrderAccessService {

    private final OrderRepository orderRepository;
    private final VendorRepository vendorRepository;

    public OrderAccessService(OrderRepository orderRepository, VendorRepository vendorRepository) {
        this.orderRepository = orderRepository;
        this.vendorRepository = vendorRepository;
    }

    public UserPrincipal requirePrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new UnauthorizedException("Authentication required");
        }
        return principal;
    }

    public VendorEntity requireVendorStorefront() {
        UserPrincipal principal = requirePrincipal();
        if (principal.getRole() != RoleName.VENDOR) {
            throw new ForbiddenException("Only vendors can access vendor orders");
        }
        return vendorRepository
                .findActiveByOwnerUserId(principal.getId())
                .orElseThrow(() -> new ForbiddenException("Vendor storefront not found"));
    }

    public OrderEntity requireReadable(UUID id) {
        UserPrincipal principal = requirePrincipal();
        if (principal.getRole() == RoleName.ADMIN) {
            return orderRepository.findById(id).orElseThrow(OrderNotFoundException::new);
        }
        if (principal.getRole() == RoleName.PLANNER) {
            return orderRepository.findByIdAndPlannerId(id, principal.getId()).orElseThrow(OrderNotFoundException::new);
        }
        if (principal.getRole() == RoleName.VENDOR) {
            VendorEntity vendor = requireVendorStorefront();
            return orderRepository.findByIdAndVendorId(id, vendor.getId()).orElseThrow(OrderNotFoundException::new);
        }
        throw new OrderNotFoundException();
    }

    public OrderEntity requirePlannerOwned(UUID id) {
        UserPrincipal principal = requirePrincipal();
        if (principal.getRole() != RoleName.PLANNER) {
            throw new ForbiddenException("Only the ordering planner can cancel this order");
        }
        return orderRepository.findByIdAndPlannerId(id, principal.getId()).orElseThrow(OrderNotFoundException::new);
    }

    public OrderEntity requireVendorOwned(UUID id) {
        VendorEntity vendor = requireVendorStorefront();
        return orderRepository.findByIdAndVendorId(id, vendor.getId()).orElseThrow(OrderNotFoundException::new);
    }

    public OrderEntity requireVendorOrAdmin(UUID id) {
        UserPrincipal principal = requirePrincipal();
        if (principal.getRole() == RoleName.ADMIN) {
            return orderRepository.findById(id).orElseThrow(OrderNotFoundException::new);
        }
        return requireVendorOwned(id);
    }
}
