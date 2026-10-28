package tz.go.tirdo.ciap.domain.department.dto;

import tz.go.tirdo.ciap.domain.consultant.Consultant;

public record EligibleAssigneeResponse(
        Long consultantId,
        String fullName,
        String role,
        String category,
        String skills,
        int activeAssignments,
        String availabilityStatus
) {
    public static EligibleAssigneeResponse from(Consultant consultant, String role) {
        return new EligibleAssigneeResponse(
                consultant.getId(),
                consultant.getFullName(),
                role,
                consultant.getCategory().name(),
                consultant.getSkills(),
                consultant.getActiveAssignments(),
                consultant.getAvailabilityStatus().name());
    }
}
