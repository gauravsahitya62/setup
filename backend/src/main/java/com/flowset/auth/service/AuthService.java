package com.flowset.auth.service;

import com.flowset.auth.dto.AuthResponse;
import com.flowset.auth.dto.ForgotPasswordRequest;
import com.flowset.auth.dto.LoginRequest;
import com.flowset.auth.dto.MessageResponse;
import com.flowset.auth.dto.RegisterPlannerRequest;
import com.flowset.auth.dto.RegisterVendorRequest;
import com.flowset.auth.dto.ResendVerificationRequest;
import com.flowset.auth.dto.ResetPasswordRequest;
import com.flowset.auth.dto.UserResponse;
import com.flowset.auth.dto.VerifyEmailRequest;
import com.flowset.common.RoleName;
import com.flowset.common.UserStatus;
import com.flowset.common.VendorVerificationStatus;
import com.flowset.config.CookieProperties;
import com.flowset.exceptions.BadRequestException;
import com.flowset.exceptions.ConflictException;
import com.flowset.exceptions.UnauthorizedException;
import com.flowset.notifications.EmailNotificationService;
import com.flowset.security.UserPrincipal;
import com.flowset.security.jwt.JwtService;
import com.flowset.shared.TokenHasher;
import com.flowset.users.entity.EmailVerificationTokenEntity;
import com.flowset.users.entity.PasswordResetTokenEntity;
import com.flowset.users.entity.PlannerProfileEntity;
import com.flowset.users.entity.RefreshTokenEntity;
import com.flowset.users.entity.UserEntity;
import com.flowset.users.entity.VendorProfileEntity;
import com.flowset.users.repository.AdminProfileRepository;
import com.flowset.users.repository.EmailVerificationTokenRepository;
import com.flowset.users.repository.PasswordResetTokenRepository;
import com.flowset.users.repository.PlannerProfileRepository;
import com.flowset.users.repository.RefreshTokenRepository;
import com.flowset.users.repository.UserRepository;
import com.flowset.users.repository.VendorProfileRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private static final long EMAIL_TOKEN_TTL_HOURS = 24;
    private static final long RESET_TOKEN_TTL_HOURS = 1;

    private final UserRepository userRepository;
    private final PlannerProfileRepository plannerProfileRepository;
    private final VendorProfileRepository vendorProfileRepository;
    private final AdminProfileRepository adminProfileRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final EmailNotificationService emailNotificationService;
    private final CookieProperties cookieProperties;

    public AuthService(
            UserRepository userRepository,
            PlannerProfileRepository plannerProfileRepository,
            VendorProfileRepository vendorProfileRepository,
            AdminProfileRepository adminProfileRepository,
            RefreshTokenRepository refreshTokenRepository,
            EmailVerificationTokenRepository emailVerificationTokenRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            AuthenticationManager authenticationManager,
            EmailNotificationService emailNotificationService,
            CookieProperties cookieProperties
    ) {
        this.userRepository = userRepository;
        this.plannerProfileRepository = plannerProfileRepository;
        this.vendorProfileRepository = vendorProfileRepository;
        this.adminProfileRepository = adminProfileRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.emailVerificationTokenRepository = emailVerificationTokenRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.emailNotificationService = emailNotificationService;
        this.cookieProperties = cookieProperties;
    }

    @Transactional
    public AuthResponse registerPlanner(
            RegisterPlannerRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        UserEntity user = createBaseUser(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phone(),
                request.password(),
                RoleName.PLANNER);

        PlannerProfileEntity profile = new PlannerProfileEntity();
        profile.setUser(user);
        profile.setCompanyName(trimToNull(request.companyName()));
        plannerProfileRepository.save(profile);

        issueEmailVerification(user);
        log.info("Registration success role=PLANNER userId={} email={}", user.getId(), user.getEmail());
        return issueSession(user, httpRequest, httpResponse);
    }

    @Transactional
    public AuthResponse registerVendor(
            RegisterVendorRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        UserEntity user = createBaseUser(
                request.firstName(),
                request.lastName(),
                request.email(),
                request.phone(),
                request.password(),
                RoleName.VENDOR);

        VendorProfileEntity profile = new VendorProfileEntity();
        profile.setUser(user);
        profile.setBusinessName(request.businessName().trim());
        profile.setVerificationStatus(VendorVerificationStatus.PENDING);
        vendorProfileRepository.save(profile);

        issueEmailVerification(user);
        log.info("Registration success role=VENDOR userId={} email={}", user.getId(), user.getEmail());
        return issueSession(user, httpRequest, httpResponse);
    }

    @Transactional
    public AuthResponse login(
            LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            normalizeEmail(request.email()),
                            request.password()));
            UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
            UserEntity user = userRepository
                    .findActiveById(principal.getId())
                    .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

            if (user.getStatus() == UserStatus.SUSPENDED) {
                log.warn("Login failure suspended userId={}", user.getId());
                throw new UnauthorizedException("Account is suspended");
            }

            log.info("Login success userId={} role={}", user.getId(), user.getRole());
            return issueSession(user, httpRequest, httpResponse);
        } catch (UnauthorizedException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("Login failure email={}", normalizeEmail(request.email()));
            throw new UnauthorizedException("Invalid email or password");
        }
    }

    @Transactional
    public MessageResponse logout(HttpServletRequest request, HttpServletResponse response) {
        readRefreshCookie(request).ifPresent(raw -> {
            String hash = TokenHasher.sha256(raw);
            refreshTokenRepository.findByTokenHash(hash).ifPresent(token -> {
                token.setRevokedAt(Instant.now());
                refreshTokenRepository.save(token);
            });
        });
        clearRefreshCookie(response);
        SecurityContextHolder.clearContext();
        log.info("Logout completed");
        return new MessageResponse("Logged out successfully");
    }

    @Transactional
    public AuthResponse refresh(HttpServletRequest request, HttpServletResponse response) {
        String raw = readRefreshCookie(request)
                .orElseThrow(() -> new UnauthorizedException("Refresh token missing"));

        RefreshTokenEntity stored = refreshTokenRepository
                .findByTokenHash(TokenHasher.sha256(raw))
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

        if (!stored.isActive()) {
            throw new UnauthorizedException("Refresh token expired or revoked");
        }

        try {
            jwtService.parseRefreshToken(raw);
        } catch (Exception ex) {
            stored.setRevokedAt(Instant.now());
            refreshTokenRepository.save(stored);
            throw new UnauthorizedException("Invalid refresh token");
        }

        UserEntity user = stored.getUser();
        if (user.isDeleted() || user.getStatus() == UserStatus.SUSPENDED) {
            stored.setRevokedAt(Instant.now());
            refreshTokenRepository.save(stored);
            throw new UnauthorizedException("Account is not active");
        }

        stored.setRevokedAt(Instant.now());
        refreshTokenRepository.save(stored);

        log.info("Token refresh success userId={}", user.getId());
        return issueSession(user, request, response);
    }

    @Transactional(readOnly = true)
    public UserResponse me() {
        UserPrincipal principal = currentPrincipal();
        UserEntity user = userRepository
                .findActiveById(principal.getId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        return toUserResponse(user);
    }

    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        String email = normalizeEmail(request.email());
        userRepository.findActiveByEmail(email).ifPresent(user -> {
            String raw = TokenHasher.generateRawToken();
            PasswordResetTokenEntity token = new PasswordResetTokenEntity();
            token.setUser(user);
            token.setTokenHash(TokenHasher.sha256(raw));
            token.setExpiresAt(Instant.now().plusSeconds(RESET_TOKEN_TTL_HOURS * 3600));
            passwordResetTokenRepository.save(token);
            emailNotificationService.sendPasswordReset(user.getEmail(), raw);
            log.info("Password reset requested userId={}", user.getId());
        });
        return new MessageResponse(
                "If an account exists for that email, password reset instructions have been sent");
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        PasswordResetTokenEntity token = passwordResetTokenRepository
                .findByTokenHash(TokenHasher.sha256(request.token()))
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        if (!token.isUsable()) {
            throw new BadRequestException("Invalid or expired reset token");
        }

        UserEntity user = token.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        token.setUsedAt(Instant.now());
        passwordResetTokenRepository.save(token);
        refreshTokenRepository.revokeAllActiveForUser(user.getId(), Instant.now());

        log.info("Password reset completed userId={}", user.getId());
        return new MessageResponse("Password updated successfully");
    }

    @Transactional
    public MessageResponse verifyEmail(VerifyEmailRequest request) {
        EmailVerificationTokenEntity token = emailVerificationTokenRepository
                .findByTokenHash(TokenHasher.sha256(request.token()))
                .orElseThrow(() -> new BadRequestException("Invalid or expired verification token"));

        if (!token.isUsable()) {
            throw new BadRequestException("Invalid or expired verification token");
        }

        UserEntity user = token.getUser();
        user.setEmailVerified(true);
        if (user.getStatus() == UserStatus.PENDING_VERIFICATION) {
            user.setStatus(UserStatus.ACTIVE);
        }
        userRepository.save(user);

        token.setUsedAt(Instant.now());
        emailVerificationTokenRepository.save(token);

        log.info("Email verified userId={}", user.getId());
        return new MessageResponse("Email verified successfully");
    }

    @Transactional
    public MessageResponse resendVerification(ResendVerificationRequest request) {
        String email = normalizeEmail(request.email());
        userRepository.findActiveByEmail(email).ifPresent(user -> {
            if (!user.isEmailVerified()) {
                issueEmailVerification(user);
                log.info("Verification email resent userId={}", user.getId());
            }
        });
        return new MessageResponse(
                "If an unverified account exists for that email, a verification link has been sent");
    }

    private UserEntity createBaseUser(
            String firstName,
            String lastName,
            String email,
            String phone,
            String password,
            RoleName role
    ) {
        String normalizedEmail = normalizeEmail(email);
        if (userRepository.existsActiveByEmail(normalizedEmail)) {
            throw new ConflictException("Email already exists");
        }

        UserEntity user = new UserEntity();
        user.setFirstName(firstName.trim());
        user.setLastName(lastName.trim());
        user.setEmail(normalizedEmail);
        user.setPhone(trimToNull(phone));
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setRole(role);
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        user.setEmailVerified(false);
        return userRepository.save(user);
    }

    private void issueEmailVerification(UserEntity user) {
        String raw = TokenHasher.generateRawToken();
        EmailVerificationTokenEntity token = new EmailVerificationTokenEntity();
        token.setUser(user);
        token.setTokenHash(TokenHasher.sha256(raw));
        token.setExpiresAt(Instant.now().plusSeconds(EMAIL_TOKEN_TTL_HOURS * 3600));
        emailVerificationTokenRepository.save(token);
        emailNotificationService.sendEmailVerification(user.getEmail(), raw);
    }

    private AuthResponse issueSession(
            UserEntity user,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String accessToken = jwtService.createAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = jwtService.createRefreshToken(user.getId());

        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setUser(user);
        entity.setTokenHash(TokenHasher.sha256(refreshToken));
        entity.setExpiresAt(Instant.now().plusMillis(jwtService.getRefreshExpirationMs()));
        entity.setUserAgent(truncate(request.getHeader(HttpHeaders.USER_AGENT), 512));
        entity.setIpAddress(truncate(request.getRemoteAddr(), 64));
        refreshTokenRepository.save(entity);

        writeRefreshCookie(response, refreshToken);
        return AuthResponse.bearer(accessToken, jwtService.getAccessExpirationMs() / 1000, toUserResponse(user));
    }

    private UserResponse toUserResponse(UserEntity user) {
        String companyName = switch (user.getRole()) {
            case PLANNER -> plannerProfileRepository
                    .findByUserId(user.getId())
                    .map(PlannerProfileEntity::getCompanyName)
                    .orElse(null);
            case VENDOR -> vendorProfileRepository
                    .findByUserId(user.getId())
                    .map(VendorProfileEntity::getBusinessName)
                    .orElse(null);
            case ADMIN -> adminProfileRepository
                    .findByUserId(user.getId())
                    .map(profile -> profile.getTitle())
                    .orElse(null);
        };

        return new UserResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.getStatus(),
                user.isEmailVerified(),
                companyName,
                user.getCreatedAt(),
                user.getUpdatedAt());
    }

    private void writeRefreshCookie(HttpServletResponse response, String rawToken) {
        ResponseCookie cookie = ResponseCookie.from(jwtService.getRefreshCookieName(), rawToken)
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .path("/api/v1/auth")
                .maxAge(jwtService.getRefreshExpirationMs() / 1000)
                .sameSite(cookieProperties.sameSite())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(jwtService.getRefreshCookieName(), "")
                .httpOnly(true)
                .secure(cookieProperties.secure())
                .path("/api/v1/auth")
                .maxAge(0)
                .sameSite(cookieProperties.sameSite())
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private Optional<String> readRefreshCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(c -> jwtService.getRefreshCookieName().equals(c.getName()))
                .map(Cookie::getValue)
                .filter(StringUtils::hasText)
                .findFirst();
    }

    private UserPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            throw new UnauthorizedException("Authentication required");
        }
        return principal;
    }

    private static String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }
}
