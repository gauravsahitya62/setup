package com.flowset.storage;

import com.flowset.exceptions.BadRequestException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/files")
@Tag(name = "Files")
public class FileController {

    private final StorageService storageService;
    private final StorageAccessService storageAccessService;

    public FileController(StorageService storageService, StorageAccessService storageAccessService) {
        this.storageService = storageService;
        this.storageAccessService = storageAccessService;
    }

    @GetMapping("/**")
    @Operation(summary = "Download/view a stored file")
    public ResponseEntity<Resource> get(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String prefix = "/api/v1/files/";
        int idx = uri.indexOf(prefix);
        if (idx < 0) {
            return ResponseEntity.notFound().build();
        }
        String relative = URLDecoder.decode(uri.substring(idx + prefix.length()), StandardCharsets.UTF_8);
        relative = relative.replace("\\", "/");
        if (relative.contains("..") || relative.startsWith("/")) {
            throw new BadRequestException("Invalid file path");
        }
        storageAccessService.assertCanRead(relative);
        Resource resource = storageService.loadAsResource(relative);
        String filename = relative.contains("/")
                ? relative.substring(relative.lastIndexOf('/') + 1)
                : relative;
        String safeName = filename.replace("\"", "");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + safeName + "\"")
                .header(HttpHeaders.CACHE_CONTROL, cacheControl(relative))
                .contentType(mediaTypeFor(safeName))
                .body(resource);
    }

    private static String cacheControl(String relativePath) {
        if (StorageVisibility.isPublic(relativePath)) {
            return "public, max-age=86400";
        }
        return "private, no-store";
    }

    private static MediaType mediaTypeFor(String filename) {
        String lower = filename.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            return MediaType.IMAGE_PNG;
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return MediaType.IMAGE_JPEG;
        }
        if (lower.endsWith(".gif")) {
            return MediaType.IMAGE_GIF;
        }
        if (lower.endsWith(".webp")) {
            return MediaType.parseMediaType("image/webp");
        }
        if (lower.endsWith(".pdf")) {
            return MediaType.APPLICATION_PDF;
        }
        if (lower.endsWith(".txt") || lower.endsWith(".csv")) {
            return MediaType.TEXT_PLAIN;
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
