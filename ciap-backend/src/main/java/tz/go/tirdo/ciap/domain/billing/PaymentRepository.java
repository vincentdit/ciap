package tz.go.tirdo.ciap.domain.billing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByControlNumber(String controlNumber);
    List<Payment> findByRequestIdOrderByIdDesc(Long requestId);
    boolean existsByControlNumber(String controlNumber);
}
