package tz.go.tirdo.ciap.domain.project.dto;

import tz.go.tirdo.ciap.domain.project.Project;
import tz.go.tirdo.ciap.domain.project.ProjectStatus;

import java.time.LocalDate;
import java.util.List;

public record ProjectResponse(
        Long id,
        String projectCode,
        Long requestId,
        String requestReference,
        String name,
        String description,
        ProjectStatus status,
        int progressPercent,
        Long leadConsultantId,
        String leadConsultantName,
        LocalDate plannedStartDate,
        LocalDate plannedEndDate,
        LocalDate actualStartDate,
        LocalDate actualEndDate,
        List<TaskResponse> tasks,
        List<MilestoneResponse> milestones
) {
    public static ProjectResponse from(Project p) {
        return new ProjectResponse(
                p.getId(), p.getProjectCode(),
                p.getRequest().getId(), p.getRequest().getReferenceNo(),
                p.getName(), p.getDescription(), p.getStatus(), p.getProgressPercent(),
                p.getLeadConsultant() != null ? p.getLeadConsultant().getId() : null,
                p.getLeadConsultant() != null ? p.getLeadConsultant().getFullName() : null,
                p.getPlannedStartDate(), p.getPlannedEndDate(),
                p.getActualStartDate(), p.getActualEndDate(),
                p.getTasks().stream().map(TaskResponse::from).toList(),
                p.getMilestones().stream().map(MilestoneResponse::from).toList());
    }
}
