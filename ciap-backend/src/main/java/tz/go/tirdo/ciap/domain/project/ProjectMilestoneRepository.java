package tz.go.tirdo.ciap.domain.project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectMilestoneRepository extends JpaRepository<ProjectMilestone, Long> {
    Optional<ProjectMilestone> findByIdAndProjectId(Long id, Long projectId);
}
