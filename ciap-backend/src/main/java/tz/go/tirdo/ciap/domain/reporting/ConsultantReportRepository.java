package tz.go.tirdo.ciap.domain.reporting;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import tz.go.tirdo.ciap.domain.consultant.Consultant;

import java.util.List;

public interface ConsultantReportRepository extends Repository<Consultant, Long> {

    @Query("select count(c) from Consultant c")
    long totalConsultants();

    @Query("select coalesce(avg(c.activeAssignments), 0) from Consultant c")
    double averageActiveAssignments();

    @Query("select c.availabilityStatus, count(c) from Consultant c group by c.availabilityStatus")
    List<Object[]> countByAvailability();

    /** All consultants ordered by load desc; service trims to the top N. */
    @Query("select c.id, c.fullName, c.category, c.activeAssignments from Consultant c order by c.activeAssignments desc")
    List<Object[]> consultantsByLoad();
}
