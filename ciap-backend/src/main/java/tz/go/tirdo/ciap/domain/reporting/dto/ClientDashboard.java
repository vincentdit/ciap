package tz.go.tirdo.ciap.domain.reporting.dto;

import java.util.List;

public record ClientDashboard(
        long totalClients,
        List<StatusCount> byIndustry,
        List<StatusCount> byType
) {}
