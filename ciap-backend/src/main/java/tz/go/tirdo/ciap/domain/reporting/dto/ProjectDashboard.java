package tz.go.tirdo.ciap.domain.reporting.dto;

import java.util.List;

public record ProjectDashboard(
        long total,
        long active,
        long completed,
        double averageProgressPercent,
        List<StatusCount> byStatus
) {}
