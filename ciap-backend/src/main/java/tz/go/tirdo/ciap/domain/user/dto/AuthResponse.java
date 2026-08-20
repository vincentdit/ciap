package tz.go.tirdo.ciap.domain.user.dto;

import java.util.List;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresInSeconds,
        String email,
        String fullName,
        List<String> roles
) {}
