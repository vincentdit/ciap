package tz.go.tirdo.ciap.domain.project;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {
    boolean existsByRequestId(Long requestId);
    Optional<Project> findByRequestId(Long requestId);
    Page<Project> findByStatus(ProjectStatus status, Pageable pageable);
    boolean existsByProjectCode(String projectCode);
}
