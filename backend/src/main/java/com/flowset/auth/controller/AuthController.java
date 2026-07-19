package com.flowset.auth.controller;

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
import com.flowset.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Authentication", description = "Registration, login, tokens, and account recovery")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register/planner")
    @SecurityRequirements
    @Operation(summary = "Register a planner account")
    public ResponseEntity<AuthResponse> registerPlanner(
            @Valid @RequestBody RegisterPlannerRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.registerPlanner(request, httpRequest, httpResponse));
    }

    @PostMapping("/register/vendor")
    @SecurityRequirements
    @Operation(summary = "Register a vendor account")
    public ResponseEntity<AuthResponse> registerVendor(
            @Valid @RequestBody RegisterVendorRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.registerVendor(request, httpRequest, httpResponse));
    }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Login with email and password")
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse
    ) {
        return ResponseEntity.ok(authService.login(request, httpRequest, httpResponse));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and revoke refresh token")
    public ResponseEntity<MessageResponse> logout(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        return ResponseEntity.ok(authService.logout(request, response));
    }

    @PostMapping("/refresh")
    @SecurityRequirements
    @Operation(summary = "Refresh access token using httpOnly refresh cookie")
    public ResponseEntity<AuthResponse> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        return ResponseEntity.ok(authService.refresh(request, response));
    }

    @PostMapping("/forgot-password")
    @SecurityRequirements
    @Operation(summary = "Request password reset email (placeholder delivery)")
    public ResponseEntity<MessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request
    ) {
        return ResponseEntity.ok(authService.forgotPassword(request));
    }

    @PostMapping("/reset-password")
    @SecurityRequirements
    @Operation(summary = "Reset password with token")
    public ResponseEntity<MessageResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        return ResponseEntity.ok(authService.resetPassword(request));
    }

    @PostMapping("/verify-email")
    @SecurityRequirements
    @Operation(summary = "Verify email with token (placeholder delivery)")
    public ResponseEntity<MessageResponse> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request
    ) {
        return ResponseEntity.ok(authService.verifyEmail(request));
    }

    @PostMapping("/resend-verification")
    @SecurityRequirements
    @Operation(summary = "Resend email verification (placeholder delivery)")
    public ResponseEntity<MessageResponse> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request
    ) {
        return ResponseEntity.ok(authService.resendVerification(request));
    }

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user")
    public ResponseEntity<UserResponse> me() {
        return ResponseEntity.ok(authService.me());
    }
}
