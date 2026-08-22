package com.flowset.admin.service;

import com.flowset.admin.dto.AdminUserResponse;
import com.flowset.admin.dto.AuditLogResponse;
import com.flowset.audit.entity.AuditLogEntity;
import com.flowset.audit.repository.AuditLogRepository;
import com.flowset.auth.dto.ForgotPasswordRequest;
import com.flowset.auth.dto.MessageResponse;
import com.flowset.auth.service.AuthService;
import com.flowset.common.RoleName;
import com.flowset.common.UserStatus;
import com.flowset.exceptions.BadRequestException;
import com.flowset.exceptions.ForbiddenException;
import com.flowset.exceptions.NotFoundException;
import com.flowset.exceptions.UnauthorizedException;
import com.flowset.security.UserPrincipal;
import com.flowset.shared.dto.PageResponse;
import com.flowset.users.entity.UserEntity;
import com.flowset.users.repository.RefreshTokenRepository;
import com.flowset.users.repository.UserRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import com.flowset.audit.AuditLogService;

@Service
public class AdminUserService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AuthService authService;
    private final AuditLogService auditLogService;
    private final AuditLogRepository auditLogRepository;

    public AdminUserService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            AuthService authService,
            AuditLogService auditLogService,
            AuditLogRepository auditLogRepository
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.authService = authService;
        this.auditLogService = auditLogService;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminUserResponse> list(
            String q, RoleName role, UserStatus status, Boolean pendingVerification, int page, int size) {
        Specification<UserEntity> spec = (root, query, cb) -> cb.isNull(root.get("deletedAt"));
        if (role != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("role"), role));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), status));
        }
        if (Boolean.TRUE.equals(pendingVerification)) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), UserStatus.PENDING_VERIFICATION));
        }
        if (StringUtils.hasText(q)) {
            String pattern = "%" + q.trim().toLowerCase() + "%";
            spec = spec.and((root, query, cb) -> cb.or(
                    cb.like(cb.lower(root.get("firstName")), pattern),
                    cb.like(cb.lower(root.get("lastName")), pattern),
                    cb.like(cb.lower(root.get("email")), pattern),
                    cb.like(cb.lower(cb.coalesce(root.get("phone"), "")), pattern)));
        }
        Page<UserEntity> result = userRepository.findAll(
                spec,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by("createdAt").descending()));
        return new PageResponse<>(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public AdminUserResponse get(UUID id) {
        return toResponse(requireUser(id));
    }

    @Transactional
    public AdminUserResponse suspend(UUID id, String reason) {
        UserPrincipal principal = requireAdmin();
        if (principal.getId().equals(id)) {
            throw new ForbiddenException("Admins cannot suspend their own account");
        }
        if (!StringUtils.hasText(reason)) {
            throw new BadRequestException("A reason is required to suspend a user");
        }
        UserEntity user = requireUser(id);
        user.setStatus(UserStatus.SUSPENDED);
        userRepository.save(user);
        refreshTokenRepository.revokeAllActiveForUser(user.getId(), Instant.now());
        auditLogService.log("USER_SUSPENDED", principal.getId().toString(), id.toString(), reason.trim());
        return toResponse(user);
    }

    @Transactional
    public AdminUserResponse reactivate(UUID id) {
        UserPrincipal principal = requireAdmin();
        UserEntity user = requireUser(id);
        user.setStatus(UserStatus.ACTIVE);
        userRepository.save(user);
        auditLogService.log("USER_REACTIVATED", principal.getId().toString(), id.toString(), user.getEmail());
        return toResponse(user);
    }

    @Transactional
    public MessageResponse requestPasswordReset(UUID id) {
        UserPrincipal principal = requireAdmin();
        UserEntity user = requireUser(id);
        MessageResponse response = authService.forgotPassword(new ForgotPasswordRequest(user.getEmail()));
        auditLogService.log("PASSWORD_RESET_REQUESTED", principal.getId().toString(), id.toString(), user.getEmail());
        return response;
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditLogResponse> activity(UUID id, int page, int size) {
        requireUser(id);
        Specification<AuditLogEntity> spec = (root, query, cb) -> cb.equal(root.get("actorUserId"), id);
        Page<AuditLogEntity> result = auditLogRepository.findAll(
                spec,
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by("createdAt").descending()));
        return new PageResponse<>(
                result.getContent().stream().map(this::toAudit).toList(),
                result.getNumber(),
                result.getSize(),
                result.getTotalElements(),
                result.getTotalPages());
    }

    private UserEntity requireUser(UUID id) {
        return userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
    }

    private AdminUserResponse toResponse(UserEntity user) {
        return new AdminUserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getStatus(),
                user.isEmailVerified(),
                user.getCreatedAt(),
                user.getLastLoginAt(),
                user.getDeletedAt());
    }

    private AuditLogResponse toAudit(AuditLogEntity entity) {
        return new AuditLogResponse(
                entity.getId(),
                entity.getActorUserId(),
                null,
                entity.getAction(),
                entity.getResourceType(),
                entity.getResourceId(),
                entity.getIpAddress(),
                entity.getUserAgent(),
                entity.getMetadata(),
                entity.getCreatedAt());
    }

    private UserPrincipal requireAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new UnauthorizedException("Authentication required");
        }
        if (principal.getRole() != RoleName.ADMIN) {
            throw new ForbiddenException("Only admins can manage users");
        }
        return principal;
    }
}
