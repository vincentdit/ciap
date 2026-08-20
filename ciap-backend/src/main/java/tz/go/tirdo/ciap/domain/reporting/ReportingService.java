package tz.go.tirdo.ciap.domain.reporting;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.ciap.domain.reporting.dto.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Assembles read-only BI dashboards (Module 12). All heavy aggregation is done in the
 * database via the reporting repositories; this service only shapes the results.
 */
@Service
public class ReportingService {

    private static final String CURRENCY = "TZS";
    private static final ZoneId ZONE = ZoneId.of("Africa/Dar_es_Salaam");

    private final ReportingRepository revenue;
    private final ConsultancyReportRepository requests;
    private final ProjectReportRepository projects;
    private final ClientReportRepository clients;
    private final ConsultantReportRepository consultants;

    public ReportingService(ReportingRepository revenue,
                            ConsultancyReportRepository requests,
                            ProjectReportRepository projects,
                            ClientReportRepository clients,
                            ConsultantReportRepository consultants) {
        this.revenue = revenue;
        this.requests = requests;
        this.projects = projects;
        this.clients = clients;
        this.consultants = consultants;
    }

    @Transactional(readOnly = true)
    public ExecutiveSummary executiveSummary() {
        return new ExecutiveSummary(
                clients.totalClients(),
                requests.totalRequests(),
                requests.totalRequests() - requests.closedRequests(),
                projects.activeProjects(),
                consultants.totalConsultants(),
                revenue.totalCollected(),
                CURRENCY);
    }

    @Transactional(readOnly = true)
    public RevenueDashboard revenue() {
        Instant startOfYear = LocalDate.now(ZONE).withDayOfYear(1).atStartOfDay(ZONE).toInstant();
        Instant twelveMonthsAgo = YearMonth.now(ZONE).minusMonths(11)
                .atDay(1).atStartOfDay(ZONE).toInstant();

        return new RevenueDashboard(
                CURRENCY,
                revenue.totalCollected(),
                revenue.totalPendingBilled(),
                revenue.paidCount(),
                revenue.pendingCount(),
                revenue.collectedSince(startOfYear),
                monthlyCollected(twelveMonthsAgo));
    }

    private List<RevenueDashboard.MonthlyRevenue> monthlyCollected(Instant since) {
        // Seed the last 12 months with zero so the series is continuous.
        Map<YearMonth, BigDecimal> buckets = new LinkedHashMap<>();
        YearMonth cursor = YearMonth.now(ZONE).minusMonths(11);
        for (int i = 0; i < 12; i++) {
            buckets.put(cursor, BigDecimal.ZERO);
            cursor = cursor.plusMonths(1);
        }

        for (Object[] row : revenue.paidPaymentsSince(since)) {
            Instant paidAt = (Instant) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            if (paidAt == null) continue;
            YearMonth ym = YearMonth.from(paidAt.atZone(ZONE));
            buckets.computeIfPresent(ym, (k, v) -> v.add(amount));
        }

        List<RevenueDashboard.MonthlyRevenue> out = new ArrayList<>();
        buckets.forEach((ym, amt) -> out.add(new RevenueDashboard.MonthlyRevenue(ym.toString(), amt)));
        return out;
    }

    @Transactional(readOnly = true)
    public ProjectDashboard projects() {
        return new ProjectDashboard(
                projects.totalProjects(),
                projects.activeProjects(),
                projects.completedProjects(),
                round1(projects.averageProgress()),
                toStatusCounts(projects.countByStatus()));
    }

    @Transactional(readOnly = true)
    public RequestPipelineDashboard requestPipeline() {
        long total = requests.totalRequests();
        long closed = requests.closedRequests();
        return new RequestPipelineDashboard(
                total,
                total - closed,
                closed,
                toStatusCounts(requests.countByStatus()),
                toStatusCounts(requests.countByType()));
    }

    @Transactional(readOnly = true)
    public ClientDashboard clients() {
        return new ClientDashboard(
                clients.totalClients(),
                toStatusCounts(clients.countByIndustry()),
                toStatusCounts(clients.countByType()));
    }

    @Transactional(readOnly = true)
    public ExpertUtilizationDashboard expertUtilization() {
        List<StatusCount> byAvail = toStatusCounts(consultants.countByAvailability());
        long available = byAvail.stream().filter(s -> "AVAILABLE".equals(s.label())).mapToLong(StatusCount::count).sum();
        long fullyBooked = byAvail.stream().filter(s -> "FULLY_BOOKED".equals(s.label())).mapToLong(StatusCount::count).sum();

        List<ExpertUtilizationDashboard.ConsultantLoad> topByLoad = new ArrayList<>();
        for (Object[] row : consultants.consultantsByLoad()) {
            if (topByLoad.size() >= 10) break;
            topByLoad.add(new ExpertUtilizationDashboard.ConsultantLoad(
                    (Long) row[0],
                    (String) row[1],
                    String.valueOf(row[2]),
                    ((Number) row[3]).intValue()));
        }

        return new ExpertUtilizationDashboard(
                consultants.totalConsultants(),
                available,
                fullyBooked,
                round1(consultants.averageActiveAssignments()),
                byAvail,
                topByLoad);
    }

    // ---- helpers ----

    private static List<StatusCount> toStatusCounts(List<Object[]> rows) {
        List<StatusCount> out = new ArrayList<>();
        for (Object[] row : rows) {
            String label = row[0] == null ? "UNKNOWN" : row[0].toString();
            long count = ((Number) row[1]).longValue();
            out.add(new StatusCount(label, count));
        }
        return out;
    }

    private static double round1(double v) {
        return Math.round(v * 10.0) / 10.0;
    }
}
