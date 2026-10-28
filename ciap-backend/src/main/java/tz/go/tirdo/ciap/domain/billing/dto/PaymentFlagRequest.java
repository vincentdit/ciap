package tz.go.tirdo.ciap.domain.billing.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Reason is required when Finance cannot confirm a pending control number. */
public record PaymentFlagRequest(
        @NotBlank @Size(max = 500) String reason
) {}