package tz.go.tirdo.ciap.domain.consultancy.dto;

import jakarta.validation.constraints.NotNull;

public record AssignDto(
        @NotNull Long consultantId
) {}
