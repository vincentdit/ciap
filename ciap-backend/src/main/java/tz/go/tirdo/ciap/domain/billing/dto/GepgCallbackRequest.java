package tz.go.tirdo.ciap.domain.billing.dto;

import jakarta.validation.constraints.NotBlank;

/** Simplified GePG settlement callback payload. */
public record GepgCallbackRequest(
        @NotBlank String controlNumber,
        @NotBlank String gepgReference
) {}
