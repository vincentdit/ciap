package tz.go.tirdo.ciap.domain.billing;

public enum PaymentStatus {
    PENDING,    // control number issued, awaiting GePG confirmation
    PAID,
    FAILED,
    CANCELLED
}
