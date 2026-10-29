package tz.go.tirdo.ciap.domain.training.dto;

import tz.go.tirdo.ciap.domain.billing.PaymentStatus;
import tz.go.tirdo.ciap.domain.training.TrainingPayment;

import java.math.BigDecimal;
import java.time.Instant;

public record TrainingPaymentResponse(
        Long id,
        String controlNumber,
        Long registrationId,
        Long eventId,
        String eventTitle,
        String registrantEmail,
        BigDecimal amount,
        String currency,
        PaymentStatus status,
        String gepgReference,
        Instant paidAt
) {
    public static TrainingPaymentResponse from(TrainingPayment payment) {
        var registration = payment.getRegistration();
        return new TrainingPaymentResponse(
                payment.getId(), payment.getControlNumber(), registration.getId(),
                registration.getEvent().getId(), registration.getEvent().getTitle(),
                registration.getRegistrantEmail(), payment.getAmount(), payment.getCurrency(),
                payment.getStatus(), payment.getGepgReference(), payment.getPaidAt());
    }
}
