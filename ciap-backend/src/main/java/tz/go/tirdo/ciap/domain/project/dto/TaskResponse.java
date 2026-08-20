package tz.go.tirdo.ciap.domain.project.dto;

import tz.go.tirdo.ciap.domain.project.ProjectTask;
import tz.go.tirdo.ciap.domain.project.TaskStatus;

import java.time.Instant;
import java.time.LocalDate;

public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        Long assigneeId,
        String assigneeName,
        LocalDate dueDate,
        Instant completedAt
) {
    public static TaskResponse from(ProjectTask t) {
        return new TaskResponse(
                t.getId(), t.getTitle(), t.getDescription(), t.getStatus(),
                t.getAssignee() != null ? t.getAssignee().getId() : null,
                t.getAssignee() != null ? t.getAssignee().getFullName() : null,
                t.getDueDate(), t.getCompletedAt());
    }
}
