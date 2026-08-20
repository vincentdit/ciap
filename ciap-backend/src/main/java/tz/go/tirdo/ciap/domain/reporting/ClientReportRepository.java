package tz.go.tirdo.ciap.domain.reporting;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import tz.go.tirdo.ciap.domain.client.Client;

import java.util.List;

public interface ClientReportRepository extends Repository<Client, Long> {

    @Query("select count(c) from Client c")
    long totalClients();

    @Query("select c.industryCategory, count(c) from Client c group by c.industryCategory")
    List<Object[]> countByIndustry();

    @Query("select c.clientType, count(c) from Client c group by c.clientType")
    List<Object[]> countByType();
}
