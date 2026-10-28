package tz.go.tirdo.ciap.domain.department;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequestAssignmentRepository extends JpaRepository<RequestAssignment, Long> {
    List<RequestAssignment> findByRequestIdOrderByIdAsc(Long requestId);
}
