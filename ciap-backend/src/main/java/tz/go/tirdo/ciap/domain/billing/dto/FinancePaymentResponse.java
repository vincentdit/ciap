package tz.go.tirdo.ciap.domain.billing.dto;

import tz.go.tirdo.ciap.domain.billing.Payment;
import tz.go.tirdo.ciap.domain.billing.PaymentStatus;
import tz.go.tirdo.ciap.domain.consultancy.RequestStatus;

import java.math.BigDecimal;
import java.time.Instant;

/** Finance queue row. Confirmation notes stay on this staff-only response. */
public record FinancePaymentResponse(
        Long id,
        String controlNumber,
        Long requestId,
        String requestReference,
        String clientName,
        String quotationNo,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String gepgReference,
        Instant paidAt,
        Instant createdAt,
        String confirmedBy,
        String confirmationNote,
        RequestStatus requestStatus,
        boolean mismatch
) {
    public static FinancePaymentResponse from(Payment payment) {
        var request = payment.getRequest();
        boolean mismatch = (payment.getStatus() == PaymentStatus.PENDING && request.getStatus() == RequestStatus.PAID)
                || (payment.getStatus() == PaymentStatus.PAID && request.getStatus() == RequestStatus.AWAITING_PAYMENT);
        return new FinancePaymentResponse(
                payment.getId(),
                payment.getControlNumber(),
                request.getId(),
                request.getReferenceNo(),
                request.getClient().getCompanyName(),
                payment.getQuotation() != null ? payment.getQuotation().getQuotationNo() : null,
                payment.getAmount(),
                payment.getCurrency(),
                payment.getStatus(),
                payment.getGepgReference(),
                payment.getPaidAt(),
                payment.getCreatedAt(),
                payment.getConfirmedBy(),
                payment.getConfirmationNote(),
                request.getStatus(),
                mismatch);
    }
}
