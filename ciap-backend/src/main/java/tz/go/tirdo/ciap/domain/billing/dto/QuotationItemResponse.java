package tz.go.tirdo.ciap.domain.billing.dto;

import tz.go.tirdo.ciap.domain.billing.QuotationItem;

import java.math.BigDecimal;

public record QuotationItemResponse(
        Long id,
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal
) {
    public static QuotationItemResponse from(QuotationItem i) {
        return new QuotationItemResponse(i.getId(), i.getDescription(),
                i.getQuantity(), i.getUnitPrice(), i.getLineTotal());
    }
}
