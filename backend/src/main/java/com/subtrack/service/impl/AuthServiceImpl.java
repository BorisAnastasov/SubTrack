package com.subtrack.service.impl;

import com.subtrack.constant.MessageConstants;
import com.subtrack.constant.RoleConstants;
import com.subtrack.dto.request.LoginRequest;
import com.subtrack.dto.request.RefreshTokenRequest;
import com.subtrack.dto.request.RegisterRequest;
import com.subtrack.dto.response.AuthResponse;
import com.subtrack.model.RefreshToken;
import com.subtrack.model.Role;
import com.subtrack.model.User;
import com.subtrack.repository.RoleRepository;
import com.subtrack.repository.UserRepository;
import com.subtrack.security.CustomUserDetails;
import com.subtrack.security.JwtService;
import com.subtrack.service.AuthService;
import com.subtrack.service.RefreshTokenService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException(MessageConstants.ERROR_EMAIL_ALREADY_TAKEN);
        }

        Role userRole = roleRepository.findByName(RoleConstants.ROLE_USER.getRoleName())
                .orElseThrow(() -> new IllegalStateException(MessageConstants.ERROR_ROLE_NOT_FOUND));

        User user = new User();
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRoles(new HashSet<>(Set.of(userRole)));
        user.setPremium(false);

        User savedUser = userRepository.save(user);

        return getAuthResponse(savedUser);
    }

    @NonNull
    private AuthResponse getAuthResponse(User savedUser) {
        CustomUserDetails userDetails = new CustomUserDetails(savedUser);
        String accessToken = jwtService.generateAccessToken(userDetails);
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(savedUser);

        List<String> roles = savedUser.getRoles().stream().map(Role::getName).toList();
        return new AuthResponse(accessToken, refreshToken.getToken(), savedUser.getId(),
                savedUser.getEmail(), savedUser.isPremium(), roles);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException(MessageConstants.ERROR_INVALID_CREDENTIALS));

        return getAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        return refreshTokenService.findByToken(request.refreshToken())
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    CustomUserDetails userDetails = new CustomUserDetails(user);
                    String accessToken = jwtService.generateAccessToken(userDetails);
                    List<String> roles = user.getRoles().stream().map(Role::getName).toList();
                    return new AuthResponse(accessToken, request.refreshToken(), user.getId(),
                            user.getEmail(), user.isPremium(), roles);
                })
                .orElseThrow(() -> new IllegalArgumentException(MessageConstants.ERROR_REFRESH_TOKEN_NOT_FOUND));
    }

    @Override
    @Transactional
    public void logout(Long userId) {
        refreshTokenService.deleteByUserId(userId);
    }
}