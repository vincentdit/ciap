package tz.go.tirdo.ciap.domain.training;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import tz.go.tirdo.ciap.domain.billing.PaymentStatus;

import java.util.List;
import java.util.Optional;

public interface TrainingPaymentRepository extends JpaRepository<TrainingPayment, Long> {
    boolean existsByControlNumber(String controlNumber);
    Optional<TrainingPayment> findByControlNumber(String controlNumber);
    Optional<TrainingPayment> findByRegistrationId(Long registrationId);
    List<TrainingPayment> findByStatusOrderByIdDesc(PaymentStatus status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from TrainingPayment p where p.controlNumber = :controlNumber")
    Optional<TrainingPayment> findByControlNumberForUpdate(@Param("controlNumber") String controlNumber);
}
