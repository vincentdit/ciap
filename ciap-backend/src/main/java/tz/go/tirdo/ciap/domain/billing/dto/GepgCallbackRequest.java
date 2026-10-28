package tz.go.tirdo.ciap.domain.billing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Settlement confirmation. The note records why a staff member confirmed it. */
public record GepgCallbackRequest(
        @NotBlank String controlNumber,
        @NotBlank String gepgReference,
        @Size(max = 500) String confirmationNote
) {}
