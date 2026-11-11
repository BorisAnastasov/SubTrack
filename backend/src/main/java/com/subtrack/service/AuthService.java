package com.subtrack.service;

import com.subtrack.dto.request.LoginRequest;
import com.subtrack.dto.request.RefreshTokenRequest;
import com.subtrack.dto.request.RegisterRequest;
import com.subtrack.dto.response.AuthResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    AuthResponse refreshToken(RefreshTokenRequest request);
    void logout(Long userId);
}