package com.smartenglish.config;

import com.smartenglish.entity.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        if (keyBytes.length < 32) {
            // Nếu secret không phải Base64 32 bytes hợp lệ, fallback sang UTF-8 bytes
            keyBytes = jwtSecret.getBytes();
        }
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Sinh JWT Token chứa đầy đủ claims theo yêu cầu:
     * - sub: email
     * - userId: ID của học viên/admin
     * - role: ROLE_ADMIN hoặc ROLE_STUDENT
     * - isPlacementCompleted: boolean
     */
    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);

        String roleName = user.getRole() != null ? user.getRole().getRoleName() : "ROLE_STUDENT";

        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getUserId());
        claims.put("role", roleName);
        claims.put("isPlacementCompleted", Boolean.TRUE.equals(user.getIsPlacementCompleted()));

        return Jwts.builder()
                .subject(user.getEmail())
                .claims(claims)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public String getEmailFromToken(String token) {
        return getClaims(token).getSubject();
    }

    public Long getUserIdFromToken(String token) {
        Object userIdObj = getClaims(token).get("userId");
        if (userIdObj instanceof Number number) {
            return number.longValue();
        }
        return null;
    }

    public String getRoleFromToken(String token) {
        return (String) getClaims(token).get("role");
    }

    public Boolean getIsPlacementCompletedFromToken(String token) {
        Object val = getClaims(token).get("isPlacementCompleted");
        return val instanceof Boolean b ? b : false;
    }

    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (MalformedJwtException ex) {
            log.error("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.error("Expired JWT token: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.error("Unsupported JWT token: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.error("JWT claims string is empty: {}", ex.getMessage());
        } catch (Exception ex) {
            log.error("JWT validation failed: {}", ex.getMessage());
        }
        return false;
    }
}

