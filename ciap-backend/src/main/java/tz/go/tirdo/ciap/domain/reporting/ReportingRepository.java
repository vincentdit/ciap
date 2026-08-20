package tz.go.tirdo.ciap.domain.reporting;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import tz.go.tirdo.ciap.domain.billing.Payment;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Read-only aggregation queries for BI dashboards. Bound to the Payment entity so Spring
 * Data can create the repository; all queries are explicit JPQL across the relevant tables.
 */
public interface ReportingRepository extends Repository<Payment, Long> {

    // ---- Revenue (payments) ----

    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.status = tz.go.tirdo.ciap.domain.billing.PaymentStatus.PAID")
    BigDecimal totalCollected();

    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.status = tz.go.tirdo.ciap.domain.billing.PaymentStatus.PENDING")
    BigDecimal totalPendingBilled();

    @Query("select count(p) from Payment p where p.status = tz.go.tirdo.ciap.domain.billing.PaymentStatus.PAID")
    long paidCount();

    @Query("select count(p) from Payment p where p.status = tz.go.tirdo.ciap.domain.billing.PaymentStatus.PENDING")
    long pendingCount();

    @Query("select coalesce(sum(p.amount), 0) from Payment p " +
           "where p.status = tz.go.tirdo.ciap.domain.billing.PaymentStatus.PAID and p.paidAt >= :since")
    BigDecimal collectedSince(Instant since);

    /** Paid payments with settlement timestamp, for monthly bucketing in the service. */
    @Query("select p.paidAt, p.amount from Payment p " +
           "where p.status = tz.go.tirdo.ciap.domain.billing.PaymentStatus.PAID and p.paidAt >= :since")
    List<Object[]> paidPaymentsSince(Instant since);
}
