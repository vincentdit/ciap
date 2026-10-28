package tz.go.tirdo.ciap.domain.department;

import org.springframework.data.jpa.repository.JpaRepository;
import tz.go.tirdo.ciap.domain.consultancy.ConsultancyType;

import java.util.Optional;

public interface ServiceRouteRepository extends JpaRepository<ServiceRoute, Long> {
    Optional<ServiceRoute> findByConsultancyTypeAndActiveTrue(ConsultancyType consultancyType);
    Optional<ServiceRoute> findByConsultancyType(ConsultancyType consultancyType);
}
