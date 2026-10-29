package tz.go.tirdo.ciap.domain.catalogue.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import tz.go.tirdo.ciap.domain.catalogue.PricingMethod;

import java.math.BigDecimal;

public record SaveServiceRequest(
        @NotNull Long categoryId,
        @NotBlank String name,
        String description,
        @NotNull PricingMethod pricingMethod,
        BigDecimal amount,
        String currency,
        String unitLabel,
        String conditions,
        Boolean published,
        Boolean active,
        String note
) {}
