package tz.go.tirdo.ciap.domain.billing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuotationRepository extends JpaRepository<Quotation, Long> {
    List<Quotation> findByRequestIdOrderByIdDesc(Long requestId);
    Optional<Quotation> findFirstByRequestIdAndStatusInOrderByIdDesc(Long requestId, List<QuotationStatus> statuses);
    boolean existsByQuotationNo(String quotationNo);
}
