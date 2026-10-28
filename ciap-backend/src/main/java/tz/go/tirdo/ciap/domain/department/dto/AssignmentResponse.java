package tz.go.tirdo.ciap.domain.department.dto;

import tz.go.tirdo.ciap.domain.department.RequestAssignment;

import java.time.Instant;
import java.time.LocalDate;

public record AssignmentResponse(
        Long id,
        Long consultantId,
        String consultantName,
        Long previousConsultantId,
        String previousConsultantName,
        String assignedBy,
        String note,
        String reason,
        LocalDate deadline,
        Instant createdAt
) {
    public static AssignmentResponse from(RequestAssignment row) {
        var previous = row.getPreviousConsultant();
        return new AssignmentResponse(
                row.getId(),
                row.getConsultant().getId(),
                row.getConsultant().getFullName(),
                previous == null ? null : previous.getId(),
                previous == null ? null : previous.getFullName(),
                row.getAssignedBy(),
                row.getNote(),
                row.getReason(),
                row.getDeadline(),
                row.getCreatedAt());
    }
}
