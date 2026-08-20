package tz.go.tirdo.ciap.domain.consultancy.dto;

import jakarta.validation.constraints.NotNull;

public record ReviewDto(
        @NotNull Boolean approved,
        String notes
) {}
