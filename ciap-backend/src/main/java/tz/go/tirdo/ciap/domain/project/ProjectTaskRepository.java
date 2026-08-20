package tz.go.tirdo.ciap.domain.project;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectTaskRepository extends JpaRepository<ProjectTask, Long> {
    Optional<ProjectTask> findByIdAndProjectId(Long id, Long projectId);
}
