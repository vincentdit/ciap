package tz.go.tirdo.ciap.domain.reporting.dto;

import java.util.List;

public record ExpertUtilizationDashboard(
        long totalConsultants,
        long available,
        long fullyBooked,
        double averageActiveAssignments,
        List<StatusCount> byAvailability,
        List<ConsultantLoad> topByLoad      // busiest consultants
) {
    public record ConsultantLoad(Long id, String fullName, String category, int activeAssignments) {}
}
