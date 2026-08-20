package tz.go.tirdo.ciap.domain.reporting.dto;

import java.math.BigDecimal;
import java.util.List;

public record RevenueDashboard(
        String currency,
        BigDecimal totalCollected,       // sum of PAID payments
        BigDecimal pendingBilled,        // sum of PENDING payments (issued, not settled)
        long paidCount,
        long pendingCount,
        BigDecimal collectedThisYear,
        List<MonthlyRevenue> monthlyCollected   // last 12 months, oldest first
) {
    public record MonthlyRevenue(String month, BigDecimal amount) {}
}
