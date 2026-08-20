package tz.go.tirdo.ciap.domain.project.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record CreateProjectRequest(
        @NotBlank String name,
        String description,
        LocalDate plannedStartDate,
        LocalDate plannedEndDate,
        /** Optional. Defaults to the request's assigned consultant if omitted. */
        Long leadConsultantId
) {}
