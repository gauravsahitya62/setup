package com.flowset.vendor.controller;

import com.flowset.common.VendorDocumentType;
import com.flowset.vendor.dto.DocumentResponse;
import com.flowset.vendor.service.VendorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/vendors/{vendorId}/documents")
@Tag(name = "Vendor Documents")
@PreAuthorize("hasAnyRole('VENDOR','ADMIN')")
@SecurityRequirement(name = "bearerAuth")
public class VendorDocumentController {

    private final VendorService vendorService;

    public VendorDocumentController(VendorService vendorService) {
        this.vendorService = vendorService;
    }

    @GetMapping
    @Operation(summary = "List vendor documents")
    public List<DocumentResponse> list(@PathVariable UUID vendorId) {
        return vendorService.listDocuments(vendorId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload vendor document metadata + file")
    public ResponseEntity<DocumentResponse> upload(
            @PathVariable UUID vendorId,
            @RequestPart("file") MultipartFile file,
            @RequestParam("documentType") VendorDocumentType documentType
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(vendorService.uploadDocument(vendorId, file, documentType));
    }

    @DeleteMapping("/{documentId}")
    @Operation(summary = "Delete vendor document")
    public ResponseEntity<Void> delete(@PathVariable UUID vendorId, @PathVariable UUID documentId) {
        vendorService.deleteDocument(vendorId, documentId);
        return ResponseEntity.noContent().build();
    }
}
