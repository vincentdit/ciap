package tz.go.tirdo.ciap.domain.reporting;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import tz.go.tirdo.ciap.domain.consultancy.ConsultancyRequest;

import java.util.List;

public interface ConsultancyReportRepository extends Repository<ConsultancyRequest, Long> {

    @Query("select count(r) from ConsultancyRequest r")
    long totalRequests();

    @Query("select count(r) from ConsultancyRequest r where r.status in " +
           "(tz.go.tirdo.ciap.domain.consultancy.RequestStatus.CLOSED, " +
           " tz.go.tirdo.ciap.domain.consultancy.RequestStatus.REJECTED, " +
           " tz.go.tirdo.ciap.domain.consultancy.RequestStatus.CANCELLED)")
    long closedRequests();

    @Query("select r.status, count(r) from ConsultancyRequest r group by r.status")
    List<Object[]> countByStatus();

    @Query("select r.consultancyType, count(r) from ConsultancyRequest r group by r.consultancyType")
    List<Object[]> countByType();
}
