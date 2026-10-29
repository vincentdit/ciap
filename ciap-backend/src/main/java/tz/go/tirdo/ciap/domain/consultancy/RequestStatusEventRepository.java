package tz.go.tirdo.ciap.domain.consultancy;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RequestStatusEventRepository extends JpaRepository<RequestStatusEvent, Long> {
    List<RequestStatusEvent> findByRequestIdOrderByIdAsc(Long requestId);
    Optional<RequestStatusEvent> findFirstByRequestIdAndToStatusOrderByIdDesc(Long requestId, String toStatus);
}
