package tz.go.tirdo.ciap.domain.project.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record CreateMilestoneRequest(
        @NotBlank String title,
        LocalDate dueDate
) {}
