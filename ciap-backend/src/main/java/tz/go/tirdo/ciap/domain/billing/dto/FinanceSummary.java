package tz.go.tirdo.ciap.domain.billing.dto;

/** Live finance counts. All figures come from stored requests and payments. */
public record FinanceSummary(
        long quoted,
        long pendingVerification,
        long verified,
        long failed,
        long mismatches
) {}
