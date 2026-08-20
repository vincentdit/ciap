package tz.go.tirdo.ciap.domain.project.dto;

import tz.go.tirdo.ciap.domain.project.ProjectMilestone;

import java.time.LocalDate;

public record MilestoneResponse(
        Long id,
        String title,
        LocalDate dueDate,
        boolean achieved,
        LocalDate achievedDate
) {
    public static MilestoneResponse from(ProjectMilestone m) {
        return new MilestoneResponse(
                m.getId(), m.getTitle(), m.getDueDate(), m.isAchieved(), m.getAchievedDate());
    }
}
