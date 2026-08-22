package com.flowset.admin.controller;

import com.flowset.admin.dto.AdminVendorDetailResponse;
import com.flowset.admin.dto.FeaturedRequest;
import com.flowset.admin.dto.ReasonRequest;
import com.flowset.admin.service.AdminVendorService;
import com.flowset.common.DocumentVerificationStatus;
import com.flowset.common.VendorStatus;
import com.flowset.common.VendorVerificationStatus;
import com.flowset.shared.dto.PageResponse;
import com.flowset.vendor.dto.DocumentResponse;
import com.flowset.vendor.dto.VendorResponse;
import com.flowset.vendor.service.VendorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/vendors")
@Tag(name = "Admin Vendors", description = "ADMIN authorization required. Verification, documents, and storefront moderation.")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminVendorController {

    private final AdminVendorService adminVendorService;
    private final VendorService vendorService;

    public AdminVendorController(AdminVendorService adminVendorService, VendorService vendorService) {
        this.adminVendorService = adminVendorService;
        this.vendorService = vendorService;
    }

    @GetMapping
    @Operation(summary = "List vendors including unpublished storefronts")
    public PageResponse<VendorResponse> list(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) VendorVerificationStatus verificationStatus,
            @RequestParam(required = false) VendorStatus status,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return adminVendorService.list(q, verificationStatus, status, featured, page, size);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Vendor review workspace")
    public AdminVendorDetailResponse get(@PathVariable UUID id) {
        return adminVendorService.get(id);
    }

    @PostMapping("/{id}/start-review")
    @Operation(summary = "Start verification review")
    public VendorResponse startReview(@PathVariable UUID id) {
        return vendorService.startReview(id);
    }

    @PostMapping("/{id}/verify")
    @Operation(summary = "Verify vendor")
    public VendorResponse verify(@PathVariable UUID id) {
        return vendorService.verify(id);
    }

    @PostMapping("/{id}/reject")
    @Operation(summary = "Reject vendor")
    public VendorResponse reject(@PathVariable UUID id, @Valid @RequestBody ReasonRequest request) {
        return vendorService.reject(id, request.reason());
    }

    @PostMapping("/{id}/suspend")
    @Operation(summary = "Suspend vendor")
    public VendorResponse suspend(@PathVariable UUID id, @Valid @RequestBody ReasonRequest request) {
        return vendorService.suspend(id, request.reason());
    }

    @PostMapping("/{id}/reactivate")
    @Operation(summary = "Reactivate vendor")
    public VendorResponse reactivate(@PathVariable UUID id) {
        return vendorService.reactivate(id);
    }

    @PostMapping("/{id}/feature")
    @Operation(summary = "Feature or unfeature vendor")
    public VendorResponse feature(@PathVariable UUID id, @RequestBody FeaturedRequest request) {
        return vendorService.setFeatured(id, request.featured());
    }

    @PostMapping("/{vendorId}/documents/{documentId}/approve")
    @Operation(summary = "Approve a private vendor document")
    public DocumentResponse approveDocument(@PathVariable UUID vendorId, @PathVariable UUID documentId) {
        return vendorService.moderateDocument(vendorId, documentId, DocumentVerificationStatus.VERIFIED, null);
    }

    @PostMapping("/{vendorId}/documents/{documentId}/reject")
    @Operation(summary = "Reject a private vendor document")
    public DocumentResponse rejectDocument(
            @PathVariable UUID vendorId, @PathVariable UUID documentId, @Valid @RequestBody ReasonRequest request) {
        return vendorService.moderateDocument(
                vendorId, documentId, DocumentVerificationStatus.REJECTED, request.reason());
    }

    @PostMapping("/{vendorId}/documents/{documentId}/request-resubmission")
    @Operation(summary = "Request document resubmission")
    public DocumentResponse requestResubmission(
            @PathVariable UUID vendorId, @PathVariable UUID documentId, @Valid @RequestBody ReasonRequest request) {
        return vendorService.moderateDocument(
                vendorId, documentId, DocumentVerificationStatus.RESUBMISSION_REQUESTED, request.reason());
    }
}
