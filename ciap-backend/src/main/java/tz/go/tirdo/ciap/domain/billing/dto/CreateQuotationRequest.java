package tz.go.tirdo.ciap.domain.billing.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record CreateQuotationRequest(
        @NotNull Long requestId,
        /** VAT rate as a fraction, e.g. 0.18 for 18%. Defaults to 0.18 if null. */
        @DecimalMin("0.0") @DecimalMax("1.0") BigDecimal vatRate,
        LocalDate validUntil,
        String notes,
        @NotEmpty @Valid List<QuotationItemDto> items
) {}
