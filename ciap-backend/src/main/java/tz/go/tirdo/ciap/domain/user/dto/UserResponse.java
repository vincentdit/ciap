package tz.go.tirdo.ciap.domain.user.dto;

import tz.go.tirdo.ciap.domain.user.RoleName;
import tz.go.tirdo.ciap.domain.user.User;

import java.util.List;

public record UserResponse(
        Long id,
        String email,
        String fullName,
        String phone,
        boolean enabled,
        List<RoleName> roles
) {
    public static UserResponse from(User u) {
        return new UserResponse(
                u.getId(), u.getEmail(), u.getFullName(), u.getPhone(), u.isEnabled(),
                u.getRoles().stream().map(r -> r.getName()).toList()
        );
    }
}
