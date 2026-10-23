package tz.go.tirdo.ciap.domain.consultancy;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConsultancyRequestRepository extends JpaRepository<ConsultancyRequest, Long> {
    Page<ConsultancyRequest> findByStatus(RequestStatus status, Pageable pageable);
    Page<ConsultancyRequest> findByClientId(Long clientId, Pageable pageable);
    Page<ConsultancyRequest> findByClient_User_EmailIgnoreCase(String email, Pageable pageable);
    Page<ConsultancyRequest> findByClient_User_EmailIgnoreCaseAndStatus(String email, RequestStatus status, Pageable pageable);
    Page<ConsultancyRequest> findByAssignedConsultant_User_EmailIgnoreCase(String email, Pageable pageable);
    Page<ConsultancyRequest> findByAssignedConsultant_User_EmailIgnoreCaseAndStatus(String email, RequestStatus status, Pageable pageable);
    long countByStatus(RequestStatus status);
}
