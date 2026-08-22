package com.flowset.vendor.controller;

import com.flowset.vendor.dto.PortfolioItemResponse;
import com.flowset.vendor.dto.PortfolioUpdateRequest;
import com.flowset.vendor.service.VendorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/vendors/{vendorId}/portfolio")
@Tag(name = "Vendor Portfolio")
@PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class VendorPortfolioController {

    private final VendorService vendorService;

    public VendorPortfolioController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Add portfolio image")
    public ResponseEntity<PortfolioItemResponse> add(
            @PathVariable UUID vendorId,
            @RequestPart("file") MultipartFile file,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String description
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vendorService.addPortfolio(vendorId, file, title, description));
    }

    @PutMapping("/{itemId}")
    @Operation(summary = "Update portfolio item metadata")
    public PortfolioItemResponse update(
            @PathVariable UUID vendorId,
            @PathVariable UUID itemId,
            @Valid @RequestBody PortfolioUpdateRequest request
    ) {
        return vendorService.updatePortfolio(vendorId, itemId, request);
    }

    @DeleteMapping("/{itemId}")
    @Operation(summary = "Delete portfolio item")
    public ResponseEntity<Void> delete(@PathVariable UUID vendorId, @PathVariable UUID itemId) {
        vendorService.deletePortfolio(vendorId, itemId);
        return ResponseEntity.noContent().build();
    }
}
