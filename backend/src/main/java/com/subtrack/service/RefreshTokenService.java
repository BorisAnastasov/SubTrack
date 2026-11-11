package com.subtrack.service;

import com.subtrack.model.RefreshToken;
import com.subtrack.model.User;

import java.util.Optional;

public interface RefreshTokenService {
    RefreshToken createRefreshToken(User user);
    RefreshToken verifyExpiration(RefreshToken token);
    Optional<RefreshToken> findByToken(String token);
    void deleteByUserId(Long userId);
}