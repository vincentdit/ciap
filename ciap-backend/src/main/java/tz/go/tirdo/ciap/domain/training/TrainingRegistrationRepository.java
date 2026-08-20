package tz.go.tirdo.ciap.domain.training;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TrainingRegistrationRepository extends JpaRepository<TrainingRegistration, Long> {
    long countByEventId(Long eventId);
    boolean existsByEventIdAndRegistrantEmail(Long eventId, String email);
    Optional<TrainingRegistration> findByEventIdAndRegistrantEmail(Long eventId, String email);
}
