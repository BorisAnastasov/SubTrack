package com.subtrack.controller;

import com.subtrack.constant.MessageConstants;
import com.subtrack.dto.request.EmailVerificationRequest;
import com.subtrack.dto.request.ForgotPasswordRequest;
import com.subtrack.dto.request.LoginRequest;
import com.subtrack.dto.request.RefreshTokenRequest;
import com.subtrack.dto.request.RegisterRequest;
import com.subtrack.dto.request.ResendVerificationRequest;
import com.subtrack.dto.request.ResetPasswordRequest;
import com.subtrack.dto.response.AuthResponse;
import com.subtrack.security.CustomUserDetails;
import com.subtrack.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body("Account created. Check your email for a link to verify your account.");
    }

    @PostMapping("/verify-email")
    public ResponseEntity<String> verifyEmail(@Valid @RequestBody EmailVerificationRequest request) {
        authService.verifyEmail(request.token());
        return ResponseEntity.ok("Email verified. You can now sign in.");
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<String> resendVerificationEmail(
            @Valid @RequestBody ResendVerificationRequest request) {
        authService.resendVerificationEmail(request);
        return ResponseEntity.accepted()
                .body("If this address needs verification, a new link will be sent.");
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.accepted()
                .body("If an account exists for this email, a password reset link will be sent.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok("Password reset successfully. You can now sign in.");
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails != null) {
            authService.logout(userDetails.getId());
        }
        return ResponseEntity.ok(MessageConstants.SUCCESS_LOGOUT);
    }
}