package tz.go.tirdo.ciap.domain.consultancy.dto;

import tz.go.tirdo.ciap.domain.consultancy.ConsultancyRequest;
import tz.go.tirdo.ciap.domain.consultancy.ConsultancyType;
import tz.go.tirdo.ciap.domain.consultancy.RequestStatus;
import tz.go.tirdo.ciap.domain.department.RoutingStatus;

import java.time.Instant;
import java.time.LocalDate;

public record RequestResponse(
        Long id,
        String referenceNo,
        Long clientId,
        String clientName,
        ConsultancyType consultancyType,
        String title,
        String description,
        LocalDate expectedStartDate,
        RequestStatus status,
        Long assignedConsultantId,
        String assignedConsultantName,
        String reviewNotes,
        Long departmentId,
        String departmentName,
        RoutingStatus routingStatus,
        LocalDate assignmentDeadline,
        Instant createdAt
) {
    public static RequestResponse from(ConsultancyRequest r, boolean includeInternalNotes) {
        var department = r.getDepartment();
        return new RequestResponse(
                r.getId(), r.getReferenceNo(),
                r.getClient().getId(), r.getClient().getCompanyName(),
                r.getConsultancyType(), r.getTitle(), r.getDescription(),
                r.getExpectedStartDate(), r.getStatus(),
                r.getAssignedConsultant() != null ? r.getAssignedConsultant().getId() : null,
                r.getAssignedConsultant() != null ? r.getAssignedConsultant().getFullName() : null,
                includeInternalNotes ? r.getReviewNotes() : null,
                department != null ? department.getId() : null,
                department != null ? department.getName() : null,
                r.getRoutingStatus(),
                r.getAssignmentDeadline(),
                r.getCreatedAt());
    }
}
