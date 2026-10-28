package tz.go.tirdo.ciap.domain.consultancy;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequestStatusEventRepository extends JpaRepository<RequestStatusEvent, Long> {
    List<RequestStatusEvent> findByRequestIdOrderByIdAsc(Long requestId);
}
