package tz.go.tirdo.ciap.domain.catalogue.dto;

import tz.go.tirdo.ciap.domain.catalogue.PriceLabels;
import tz.go.tirdo.ciap.domain.catalogue.PricingMethod;
import tz.go.tirdo.ciap.domain.catalogue.ServicePriceVersion;

import java.math.BigDecimal;
import java.time.Instant;

public record PriceVersionResponse(
        Long id,
        PricingMethod pricingMethod,
        BigDecimal amount,
        String currency,
        String unitLabel,
        String conditions,
        String note,
        String changedBy,
        Instant changedAt,
        String customerLabel
) {
    public static PriceVersionResponse from(ServicePriceVersion version) {
        BigDecimal amount = version.getPricingMethod().requiresAmount() ? version.getAmount() : null;
        return new PriceVersionResponse(
                version.getId(), version.getPricingMethod(), amount, version.getCurrency(),
                version.getUnitLabel(), version.getConditions(), version.getNote(),
                version.getChangedBy(), version.getChangedAt(),
                PriceLabels.customerLabel(version.getPricingMethod(), amount, version.getCurrency(), version.getUnitLabel()));
    }
}
