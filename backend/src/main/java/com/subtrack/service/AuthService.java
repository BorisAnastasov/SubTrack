package com.subtrack.service;

import com.subtrack.dto.request.LoginRequest;
import com.subtrack.dto.request.RefreshTokenRequest;
import com.subtrack.dto.request.RegisterRequest;
import com.subtrack.dto.request.ResendVerificationRequest;
import com.subtrack.dto.response.AuthResponse;

public interface AuthService {
    void register(RegisterRequest request);
    void verifyEmail(String token);
    void resendVerificationEmail(ResendVerificationRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(RefreshTokenRequest request);
    void logout(Long userId);
}