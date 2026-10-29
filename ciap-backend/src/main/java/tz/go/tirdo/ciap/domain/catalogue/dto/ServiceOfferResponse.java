package tz.go.tirdo.ciap.domain.catalogue.dto;

import tz.go.tirdo.ciap.domain.catalogue.PriceLabels;
import tz.go.tirdo.ciap.domain.catalogue.PricingMethod;
import tz.go.tirdo.ciap.domain.catalogue.ServiceOffering;
import tz.go.tirdo.ciap.domain.consultancy.ConsultancyType;

import java.math.BigDecimal;

public record ServiceOfferResponse(
        Long id,
        Long categoryId,
        String categoryName,
        String categoryCode,
        ConsultancyType consultancyType,
        String name,
        String description,
        PricingMethod pricingMethod,
        BigDecimal amount,
        String currency,
        String unitLabel,
        String conditions,
        Long priceVersionId,
        String customerLabel,
        boolean published,
        boolean active
) {
    public static ServiceOfferResponse from(ServiceOffering service, Long priceVersionId) {
        PricingMethod method = service.getPricingMethod();
        BigDecimal amount = method.requiresAmount() ? service.getAmount() : null;
        return new ServiceOfferResponse(
                service.getId(),
                service.getCategory().getId(),
                service.getCategory().getName(),
                service.getCategory().getCode(),
                service.getCategory().getConsultancyType(),
                service.getName(),
                service.getDescription(),
                method,
                amount,
                service.getCurrency(),
                service.getUnitLabel(),
                service.getConditions(),
                priceVersionId,
                PriceLabels.customerLabel(method, amount, service.getCurrency(), service.getUnitLabel()),
                service.isPublished(),
                service.isActive());
    }
}
