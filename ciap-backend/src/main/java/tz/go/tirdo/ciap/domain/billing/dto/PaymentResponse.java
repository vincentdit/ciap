package tz.go.tirdo.ciap.domain.billing.dto;

import tz.go.tirdo.ciap.domain.billing.Payment;
import tz.go.tirdo.ciap.domain.billing.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        Long id,
        String controlNumber,
        Long requestId,
        Long quotationId,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String gepgReference,
        Instant paidAt,
        String confirmedBy,
        String confirmationNote
) {
    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(
                p.getId(), p.getControlNumber(), p.getRequest().getId(),
                p.getQuotation() != null ? p.getQuotation().getId() : null,
                p.getAmount(), p.getCurrency(), p.getStatus(),
                p.getGepgReference(), p.getPaidAt(),
                p.getConfirmedBy(), p.getConfirmationNote());
    }
}
