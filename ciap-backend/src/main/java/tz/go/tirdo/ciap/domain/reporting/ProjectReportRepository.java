package tz.go.tirdo.ciap.domain.reporting;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import tz.go.tirdo.ciap.domain.project.Project;

import java.util.List;

public interface ProjectReportRepository extends Repository<Project, Long> {

    @Query("select count(p) from Project p")
    long totalProjects();

    @Query("select count(p) from Project p where p.status = tz.go.tirdo.ciap.domain.project.ProjectStatus.ACTIVE")
    long activeProjects();

    @Query("select count(p) from Project p where p.status = tz.go.tirdo.ciap.domain.project.ProjectStatus.COMPLETED")
    long completedProjects();

    @Query("select coalesce(avg(p.progressPercent), 0) from Project p")
    double averageProgress();

    @Query("select p.status, count(p) from Project p group by p.status")
    List<Object[]> countByStatus();
}
