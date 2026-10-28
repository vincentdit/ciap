package tz.go.tirdo.ciap.domain.department.dto;

public record DepartmentQueueSummary(
        long submitted,
        long underReview,
        long awaitingAssignment,
        long assigned,
        long inProgress,
        long overdue,
        long awaitingClose,
        long rejected,
        long unrouted
) {}
