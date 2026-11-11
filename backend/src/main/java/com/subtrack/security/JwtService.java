package com.subtrack.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.access-expiration-ms}")
    private long jwtAccessExpiration;

    public static final String ACCESS_COOKIE_NAME = "subtrack_access_token";
    public static final String REFRESH_COOKIE_NAME = "subtrack_refresh_token";

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateAccessToken(UserDetails userDetails) {
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtAccessExpiration))
                .signWith(getSignInKey())
                .compact();
    }

    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    private Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public ResponseCookie generateAccessCookie(String token) {
        return ResponseCookie.from(ACCESS_COOKIE_NAME, token)
                .path("/")
                .maxAge(jwtAccessExpiration / 1000)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .build();
    }

    public ResponseCookie generateRefreshCookie(String token, long durationSeconds) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, token)
                .path("/api/auth")
                .maxAge(durationSeconds)
                .httpOnly(true)
                .secure(false)
                .sameSite("Lax")
                .build();
    }

    public ResponseCookie getCleanCookie(String cookieName, String path) {
        return ResponseCookie.from(cookieName, "")
                .path(path)
                .maxAge(0)
                .httpOnly(true)
                .build();
    }

    public String getJwtFromCookies(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}