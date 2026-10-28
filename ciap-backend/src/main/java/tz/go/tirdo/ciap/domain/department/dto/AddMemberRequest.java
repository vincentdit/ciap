package tz.go.tirdo.ciap.domain.department.dto;

import jakarta.validation.constraints.NotNull;

public record AddMemberRequest(@NotNull Long userId, Boolean canReceiveWork) {}
