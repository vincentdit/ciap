package tz.go.tirdo.ciap.domain.reporting.dto;

/** Top-line KPIs for the executive landing dashboard. */
public record ExecutiveSummary(
        long totalClients,
        long totalRequests,
        long openRequests,
        long activeProjects,
        long totalConsultants,
        java.math.BigDecimal totalRevenueCollected,
        String currency
) {}
