package tz.go.tirdo.ciap.domain.user.dto;

import jakarta.validation.constraints.NotNull;
import tz.go.tirdo.ciap.domain.user.RoleName;

/** One role per account. The API rejects a missing role. */
public record UpdateRoleRequest(@NotNull RoleName role) {}
