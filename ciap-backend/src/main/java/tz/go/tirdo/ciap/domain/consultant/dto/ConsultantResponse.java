package tz.go.tirdo.ciap.domain.consultant.dto;

import tz.go.tirdo.ciap.domain.consultant.AvailabilityStatus;
import tz.go.tirdo.ciap.domain.consultant.Consultant;
import tz.go.tirdo.ciap.domain.consultant.ExpertCategory;

import java.math.BigDecimal;

public record ConsultantResponse(
        Long id,
        String fullName,
        ExpertCategory category,
        String skills,
        Integer yearsExperience,
        AvailabilityStatus availabilityStatus,
        int activeAssignments,
        Double performanceRating,
        BigDecimal billingRatePerHour
) {
    public static ConsultantResponse from(Consultant c) {
        return new ConsultantResponse(
                c.getId(), c.getFullName(), c.getCategory(), c.getSkills(),
                c.getYearsExperience(), c.getAvailabilityStatus(), c.getActiveAssignments(),
                c.getPerformanceRating(), c.getBillingRatePerHour());
    }
}
