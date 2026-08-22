package com.flowset.vendor.controller;

import com.flowset.vendor.dto.ServiceAreaRequest;
import com.flowset.vendor.dto.ServiceAreaResponse;
import com.flowset.vendor.service.VendorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vendors/{vendorId}/service-areas")
@Tag(name = "Vendor Service Areas")
public class VendorServiceAreaController {

    private final VendorService vendorService;

    public VendorServiceAreaController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    @GetMapping
    @SecurityRequirements
    @Operation(summary = "List service areas")
    public List<ServiceAreaResponse> list(@PathVariable UUID vendorId) {
        return vendorService.listServiceAreas(vendorId);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Add service area")
    public ResponseEntity<ServiceAreaResponse> create(
            @PathVariable UUID vendorId,
            @Valid @RequestBody ServiceAreaRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vendorService.addServiceArea(vendorId, request));
    }

    @PutMapping("/{areaId}")
    @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update service area")
    public ServiceAreaResponse update(
            @PathVariable UUID vendorId,
            @PathVariable UUID areaId,
            @Valid @RequestBody ServiceAreaRequest request
    ) {
        return vendorService.updateServiceArea(vendorId, areaId, request);
    }

    @DeleteMapping("/{areaId}")
    @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Delete service area")
    public ResponseEntity<Void> delete(@PathVariable UUID vendorId, @PathVariable UUID areaId) {
        vendorService.deleteServiceArea(vendorId, areaId);
        return ResponseEntity.noContent().build();
    }
}
