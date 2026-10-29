package tz.go.tirdo.ciap.domain.catalogue;

import tz.go.tirdo.ciap.domain.consultancy.ConsultancyType;

import java.math.BigDecimal;

/** The price a customer confirmed, copied onto the request. */
public record PriceSnapshot(
        Long serviceId,
        String serviceName,
        ConsultancyType consultancyType,
        PricingMethod pricingMethod,
        BigDecimal amount,
        String currency,
        Long priceVersionId,
        BigDecimal quantity,
        String customerLabel
) {}
