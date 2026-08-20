package tz.go.tirdo.ciap.domain.consultant.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import tz.go.tirdo.ciap.domain.consultant.ExpertCategory;

import java.math.BigDecimal;

public record CreateConsultantRequest(
        @NotBlank String fullName,
        @NotNull ExpertCategory category,
        String skills,
        @PositiveOrZero Integer yearsExperience,
        BigDecimal billingRatePerHour,
        Long userId
) {}
