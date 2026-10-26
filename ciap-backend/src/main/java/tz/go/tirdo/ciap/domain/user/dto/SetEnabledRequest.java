package tz.go.tirdo.ciap.domain.user.dto;

import jakarta.validation.constraints.NotNull;

public record SetEnabledRequest(@NotNull Boolean enabled) {}
