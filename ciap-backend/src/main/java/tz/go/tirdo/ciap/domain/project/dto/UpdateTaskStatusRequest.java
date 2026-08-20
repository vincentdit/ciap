package tz.go.tirdo.ciap.domain.project.dto;

import jakarta.validation.constraints.NotNull;
import tz.go.tirdo.ciap.domain.project.TaskStatus;

public record UpdateTaskStatusRequest(
        @NotNull TaskStatus status
) {}
