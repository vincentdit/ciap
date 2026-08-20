package tz.go.tirdo.ciap.domain.consultancy;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultancyRequestRepository extends JpaRepository<ConsultancyRequest, Long> {
    Page<ConsultancyRequest> findByStatus(RequestStatus status, Pageable pageable);
    Page<ConsultancyRequest> findByClientId(Long clientId, Pageable pageable);
    long countByStatus(RequestStatus status);
}
