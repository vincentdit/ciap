package tz.go.tirdo.ciap.domain.billing.dto;

import tz.go.tirdo.ciap.domain.billing.Quotation;
import tz.go.tirdo.ciap.domain.billing.QuotationStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record QuotationResponse(
        Long id,
        String quotationNo,
        Long requestId,
        String requestReference,
        QuotationStatus status,
        String currency,
        BigDecimal subtotal,
        BigDecimal vatRate,
        BigDecimal vatAmount,
        BigDecimal total,
        LocalDate validUntil,
        String notes,
        List<QuotationItemResponse> items,
        int versionNo
) {
    public static QuotationResponse from(Quotation q) {
        return new QuotationResponse(
                q.getId(), q.getQuotationNo(),
                q.getRequest().getId(), q.getRequest().getReferenceNo(),
                q.getStatus(), q.getCurrency(), q.getSubtotal(), q.getVatRate(),
                q.getVatAmount(), q.getTotal(), q.getValidUntil(), q.getNotes(),
                q.getItems().stream().map(QuotationItemResponse::from).toList(),
                q.getVersionNo());
    }
}
