package tz.go.tirdo.ciap.domain.project.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.LocalDate;

public record CreateTaskRequest(
        @NotBlank String title,
        String description,
        Long assigneeConsultantId,
        LocalDate dueDate
) {}
