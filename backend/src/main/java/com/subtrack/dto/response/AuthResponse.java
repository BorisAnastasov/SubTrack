package com.subtrack.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;

    @Builder.Default
    private String tokenType = "Bearer";

    private Long id;
    private String email;
    private boolean isPremium;
    private List<String> roles;

    public AuthResponse(String accessToken, String refreshToken, Long id, String email, boolean isPremium, List<String> roles) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = "Bearer";
        this.id = id;
        this.email = email;
        this.isPremium = isPremium;
        this.roles = roles;
    }
}