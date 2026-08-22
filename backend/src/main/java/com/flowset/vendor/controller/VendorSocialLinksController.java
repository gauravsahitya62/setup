package com.flowset.vendor.controller;

import com.flowset.vendor.dto.SocialLinksRequest;
import com.flowset.vendor.dto.SocialLinksResponse;
import com.flowset.vendor.service.VendorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/vendors/{vendorId}/social-links")
@Tag(name = "Vendor Social Links")
public class VendorSocialLinksController {

    private final VendorService vendorService;

    public VendorSocialLinksController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    @GetMapping
    @SecurityRequirements
    @Operation(summary = "Get social links")
    public SocialLinksResponse get(@PathVariable UUID vendorId) {
        return vendorService.getSocialLinks(vendorId);
    }

    @PutMapping
    @PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Upsert social links")
    public SocialLinksResponse upsert(
            @PathVariable UUID vendorId,
            @Valid @RequestBody SocialLinksRequest request
    ) {
        return vendorService.upsertSocialLinks(vendorId, request);
    }
}
