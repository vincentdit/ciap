package tz.go.tirdo.ciap.domain.consultancy;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import tz.go.tirdo.ciap.domain.department.RoutingStatus;

import java.time.LocalDate;

public interface ConsultancyRequestRepository extends JpaRepository<ConsultancyRequest, Long>,
        JpaSpecificationExecutor<ConsultancyRequest> {
    Page<ConsultancyRequest> findByStatus(RequestStatus status, Pageable pageable);
    Page<ConsultancyRequest> findByClientId(Long clientId, Pageable pageable);
    Page<ConsultancyRequest> findByClient_User_EmailIgnoreCase(String email, Pageable pageable);
    Page<ConsultancyRequest> findByClient_User_EmailIgnoreCaseAndStatus(String email, RequestStatus status, Pageable pageable);
    Page<ConsultancyRequest> findByAssignedConsultant_User_EmailIgnoreCase(String email, Pageable pageable);
    Page<ConsultancyRequest> findByAssignedConsultant_User_EmailIgnoreCaseAndStatus(String email, RequestStatus status, Pageable pageable);
    Page<ConsultancyRequest> findByDepartment_HodUser_EmailIgnoreCase(String email, Pageable pageable);
    Page<ConsultancyRequest> findByDepartment_HodUser_EmailIgnoreCaseAndStatus(String email, RequestStatus status, Pageable pageable);
    long countByStatus(RequestStatus status);
    long countByRoutingStatus(RoutingStatus routingStatus);
    long countByDepartment_HodUser_EmailIgnoreCaseAndStatus(String email, RequestStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update ConsultancyRequest r
               set r.routingStatus = tz.go.tirdo.ciap.domain.department.RoutingStatus.ROUTED
             where r.department.id = :departmentId
               and r.routingStatus = tz.go.tirdo.ciap.domain.department.RoutingStatus.NO_HOD
            """)
    int markRouted(@Param("departmentId") Long departmentId);

    @Query("""
            select count(r) from ConsultancyRequest r
             where lower(r.department.hodUser.email) = lower(:email)
               and r.assignmentDeadline < :today
               and r.status in (tz.go.tirdo.ciap.domain.consultancy.RequestStatus.ASSIGNED,
                                tz.go.tirdo.ciap.domain.consultancy.RequestStatus.IN_EXECUTION)
            """)
    long countOverdueForHod(@Param("email") String email, @Param("today") LocalDate today);

    @Query("""
            select count(r) from ConsultancyRequest r
             where r.assignmentDeadline < :today
               and r.status in (tz.go.tirdo.ciap.domain.consultancy.RequestStatus.ASSIGNED,
                                tz.go.tirdo.ciap.domain.consultancy.RequestStatus.IN_EXECUTION)
            """)
    long countOverdue(@Param("today") LocalDate today);
}
