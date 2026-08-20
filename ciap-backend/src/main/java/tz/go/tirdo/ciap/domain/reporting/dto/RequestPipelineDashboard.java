package tz.go.tirdo.ciap.domain.reporting.dto;

import java.util.List;

public record RequestPipelineDashboard(
        long total,
        long open,         // not in a terminal state (CLOSED/REJECTED/CANCELLED)
        long closed,
        List<StatusCount> byStatus,
        List<StatusCount> byType
) {}
