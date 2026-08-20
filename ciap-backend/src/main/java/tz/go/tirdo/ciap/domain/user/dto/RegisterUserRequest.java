package tz.go.tirdo.ciap.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tz.go.tirdo.ciap.domain.user.RoleName;

import java.util.Set;

/**
 * Internal-staff creation request (ADMIN only).
 */
public record RegisterUserRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String password,
        @NotBlank String fullName,
        String phone,
        @NotNull Set<RoleName> roles
) {}
