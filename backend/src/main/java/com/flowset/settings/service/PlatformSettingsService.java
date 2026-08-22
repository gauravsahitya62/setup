package com.flowset.settings.service;

import com.flowset.audit.AuditLogService;
import com.flowset.common.AuditResourceType;
import com.flowset.common.RoleName;
import com.flowset.exceptions.ForbiddenException;
import com.flowset.exceptions.NotFoundException;
import com.flowset.exceptions.UnauthorizedException;
import com.flowset.security.UserPrincipal;
import com.flowset.settings.dto.PlatformSettingsResponse;
import com.flowset.settings.dto.PlatformSettingsUpdateRequest;
import com.flowset.settings.entity.PlatformSettingsEntity;
import com.flowset.settings.repository.PlatformSettingsRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PlatformSettingsService {

    private final PlatformSettingsRepository repository;
    private final AuditLogService auditLogService;

    public PlatformSettingsService(PlatformSettingsRepository repository, AuditLogService auditLogService) {
        this.repository = repository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public PlatformSettingsEntity requireSingleton() {
        return repository
                .findById(PlatformSettingsEntity.SINGLETON_ID)
                .orElseThrow(() -> new NotFoundException("Platform settings are not initialized"));
    }

    @Transactional(readOnly = true)
    public PlatformSettingsResponse get() {
        return toResponse(requireSingleton());
    }

    @Transactional(readOnly = true)
    public int reviewEditWindowDays() {
        return requireSingleton().getReviewEditWindowDays();
    }

    @Transactional
    public PlatformSettingsResponse update(PlatformSettingsUpdateRequest request) {
        UserPrincipal principal = requireAdmin();
        PlatformSettingsEntity entity = requireSingleton();
        entity.setPlatformName(request.platformName().trim());
        entity.setSupportEmail(request.supportEmail().trim().toLowerCase());
        entity.setDefaultInventoryThreshold(request.defaultInventoryThreshold());
        entity.setReviewEditWindowDays(request.reviewEditWindowDays());
        entity.setVendorVerificationRequired(request.vendorVerificationRequired());
        entity.setMaxImageSizeBytes(request.maxImageSizeBytes());
        entity.setAllowedFileTypes(request.allowedFileTypes().trim());
        entity.setUpdatedBy(principal.getId());
        PlatformSettingsEntity saved = repository.save(entity);
        auditLogService.record(
                "PLATFORM_SETTINGS_CHANGED",
                principal.getId(),
                AuditResourceType.PLATFORM_SETTINGS,
                saved.getId(),
                "name=" + saved.getPlatformName());
        return toResponse(saved);
    }

    private PlatformSettingsResponse toResponse(PlatformSettingsEntity entity) {
        return new PlatformSettingsResponse(
                entity.getPlatformName(),
                entity.getSupportEmail(),
                entity.getDefaultInventoryThreshold(),
                entity.getReviewEditWindowDays(),
                entity.isVendorVerificationRequired(),
                entity.getMaxImageSizeBytes(),
                entity.getAllowedFileTypes(),
                entity.getUpdatedAt(),
                entity.getUpdatedBy());
    }

    private UserPrincipal requireAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new UnauthorizedException("Authentication required");
        }
        if (principal.getRole() != RoleName.ADMIN) {
            throw new ForbiddenException("Only admins can change platform settings");
        }
        return principal;
    }
}
