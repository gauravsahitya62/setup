package com.flowset.vendor.controller;

import com.flowset.shared.dto.PageResponse;
import com.flowset.vendor.dto.CreateVendorRequest;
import com.flowset.vendor.dto.UpdateVendorRequest;
import com.flowset.vendor.dto.VendorResponse;
import com.flowset.vendor.service.VendorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/vendors")
@Tag(name = "Vendors", description = "Vendor storefront management and discovery")
public class VendorController {

    private final VendorService vendorService;

    public VendorController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Create vendor storefront")
    public ResponseEntity<VendorResponse> create(@Valid @RequestBody CreateVendorRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(vendorService.create(request));
    }

    @GetMapping
    @SecurityRequirements
    @Operation(summary = "Search vendors")
    public PageResponse<VendorResponse> search(
            @RequestParam(required = false) String companyName,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String businessType,
            @RequestParam(required = false) Boolean verified,
            @RequestParam(required = false) BigDecimal minRating,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        return vendorService.search(companyName, city, state, businessType, verified, minRating, page, size);
    }

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Get current vendor storefront")
    public VendorResponse me() {
        return vendorService.getMine();
    }

    @GetMapping("/{id}")
    @SecurityRequirements
    @Operation(summary = "Get vendor by id")
    public VendorResponse getById(@PathVariable UUID id) {
        return vendorService.getById(id);
    }

    @GetMapping("/slug/{slug}")
    @SecurityRequirements
    @Operation(summary = "Get vendor by slug")
    public VendorResponse getBySlug(@PathVariable String slug) {
        return vendorService.getBySlug(slug);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Update vendor profile")
    public VendorResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateVendorRequest request) {
        return vendorService.update(id, request);
    }

    @PostMapping("/{id}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Verify a vendor storefront")
    public VendorResponse verify(@PathVariable UUID id) {
        return vendorService.verify(id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Soft-delete vendor storefront")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        vendorService.softDelete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Upload vendor logo")
    public VendorResponse uploadLogo(@PathVariable UUID id, @RequestPart("file") MultipartFile file) {
        return vendorService.uploadLogo(id, file);
    }

    @PostMapping(value = "/{id}/banner", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Upload vendor banner")
    public VendorResponse uploadBanner(@PathVariable UUID id, @RequestPart("file") MultipartFile file) {
        return vendorService.uploadBanner(id, file);
    }
}
