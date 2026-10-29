package tz.go.tirdo.ciap.domain.consultancy.dto;

import jakarta.validation.constraints.NotBlank;

public record ReasonDto(@NotBlank String reason) {}
