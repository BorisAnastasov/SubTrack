package com.subtrack.service.impl;

import com.subtrack.constant.MessageConstants;
import com.subtrack.constant.RoleConstants;
import com.subtrack.dto.request.LoginRequest;
import com.subtrack.dto.request.RefreshTokenRequest;
import com.subtrack.dto.request.RegisterRequest;
import com.subtrack.dto.request.ResendVerificationRequest;
import com.subtrack.dto.request.ForgotPasswordRequest;
import com.subtrack.dto.request.ResetPasswordRequest;
import com.subtrack.dto.response.AuthResponse;
import com.subtrack.model.EmailVerificationToken;
import com.subtrack.model.PasswordResetToken;
import com.subtrack.model.RefreshToken;
import com.subtrack.model.Role;
import com.subtrack.model.User;
import com.subtrack.repository.EmailVerificationTokenRepository;
import com.subtrack.repository.PasswordResetTokenRepository;
import com.subtrack.repository.RoleRepository;
import com.subtrack.repository.UserRepository;
import com.subtrack.security.CustomUserDetails;
import com.subtrack.security.JwtService;
import com.subtrack.service.AuthService;
import com.subtrack.service.EmailService;
import com.subtrack.service.RefreshTokenService;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final EmailVerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RoleRepository roleRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenService refreshTokenService;

    @Override
    @Transactional
    public void register(RegisterRequest request) {
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
        user.setVerified(false);

        User savedUser = userRepository.save(user);

        sendVerificationEmail(savedUser);
    }

    @Override
    @Transactional
    public void verifyEmail(String token) {
        EmailVerificationToken verificationToken = verificationTokenRepository.findByTokenHash(hashToken(token))
                .orElseThrow(() -> new IllegalArgumentException(MessageConstants.ERROR_VERIFICATION_TOKEN_INVALID));
        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException(MessageConstants.ERROR_VERIFICATION_TOKEN_EXPIRED);
        }

        User user = verificationToken.getUser();
        user.setVerified(true);
        userRepository.save(user);
        verificationTokenRepository.delete(verificationToken);
    }

    @Override
    @Transactional
    public void resendVerificationEmail(ResendVerificationRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + request.email()));

        if (user.isVerified()) {
            throw new IllegalStateException("User is already verified.");
        }

        verificationTokenRepository.findByUserId(user.getId())
                .ifPresent(existingToken -> {
                    verificationTokenRepository.delete(existingToken);
                    verificationTokenRepository.flush();
                });

        sendVerificationEmail(user);
    }

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.email()).ifPresent(user -> {
            passwordResetTokenRepository.findByUserId(user.getId())
                    .ifPresent(existingToken -> {
                        passwordResetTokenRepository.delete(existingToken);
                        passwordResetTokenRepository.flush();
                    });

            byte[] randomToken = new byte[32];
            new SecureRandom().nextBytes(randomToken);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomToken);

            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setTokenHash(hashToken(token));
            resetToken.setUser(user);
            resetToken.setExpiresAt(LocalDateTime.now().plusHours(1));
            passwordResetTokenRepository.save(resetToken);

            emailService.sendPasswordResetEmail(user.getEmail(), token);
        });
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(hashToken(request.token()))
                .orElseThrow(() -> new IllegalArgumentException("Password reset link is invalid or already used"));
        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Password reset link has expired");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        userRepository.save(user);
        passwordResetTokenRepository.delete(resetToken);
        refreshTokenService.deleteByUserId(user.getId());
    }

    private void sendVerificationEmail(User user) {
        byte[] randomToken = new byte[32];
        new SecureRandom().nextBytes(randomToken);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomToken);

        EmailVerificationToken verificationToken = new EmailVerificationToken();
        verificationToken.setTokenHash(hashToken(token));
        verificationToken.setUser(user);
        verificationToken.setExpiresAt(LocalDateTime.now().plusHours(24));
        verificationTokenRepository.save(verificationToken);

        emailService.sendVerificationEmail(user.getEmail(), token);
    }

    private String hashToken(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
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
        User existingUser = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException(MessageConstants.ERROR_INVALID_CREDENTIALS));
        if (!existingUser.isVerified()) {
            if (verificationTokenRepository.existsByUserId(existingUser.getId())) {
                throw new IllegalArgumentException(MessageConstants.ERROR_EMAIL_NOT_VERIFIED);
            }
            existingUser.setVerified(true);
            userRepository.save(existingUser);
        }

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        return getAuthResponse(existingUser);
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