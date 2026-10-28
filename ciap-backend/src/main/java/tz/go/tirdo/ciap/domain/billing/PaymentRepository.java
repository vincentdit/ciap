package tz.go.tirdo.ciap.domain.billing;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByControlNumber(String controlNumber);
    List<Payment> findByRequestIdOrderByIdDesc(Long requestId);
    boolean existsByControlNumber(String controlNumber);
    boolean existsByRequestIdAndStatus(Long requestId, PaymentStatus status);
    boolean existsByRequestIdAndStatusIn(Long requestId, Collection<PaymentStatus> statuses);
    boolean existsByGepgReferenceIgnoreCaseAndIdNot(String gepgReference, Long id);
    long countByStatus(PaymentStatus status);

    @Query("""
            select p from Payment p
            where (:status is null or p.status = :status)
              and (:from is null or p.createdAt >= :from)
              and (:to is null or p.createdAt < :to)
              and (:term is null
                   or lower(p.controlNumber) like :term
                   or lower(coalesce(p.gepgReference, '')) like :term
                   or lower(p.request.referenceNo) like :term
                   or lower(p.request.client.companyName) like :term)
              and (:onlyMismatches = false
                   or ((p.status = tz.go.tirdo.ciap.domain.billing.PaymentStatus.PENDING
                        and p.request.status = tz.go.tirdo.ciap.domain.consultancy.RequestStatus.PAID)
                       or (p.status = tz.go.tirdo.ciap.domain.billing.PaymentStatus.PAID
                           and p.request.status = tz.go.tirdo.ciap.domain.consultancy.RequestStatus.AWAITING_PAYMENT)))
            """)
    Page<Payment> search(@Param("status") PaymentStatus status,
                         @Param("from") Instant from,
                         @Param("to") Instant to,
                         @Param("term") String term,
                         @Param("onlyMismatches") boolean onlyMismatches,
                         Pageable pageable);

    @Query("""
            select count(p) from Payment p
            where (p.status = tz.go.tirdo.ciap.domain.billing.PaymentStatus.PENDING
                   and p.request.status = tz.go.tirdo.ciap.domain.consultancy.RequestStatus.PAID)
               or (p.status = tz.go.tirdo.ciap.domain.billing.PaymentStatus.PAID
                   and p.request.status = tz.go.tirdo.ciap.domain.consultancy.RequestStatus.AWAITING_PAYMENT)
            """)
    long countMismatches();
}
