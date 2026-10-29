package tz.go.tirdo.ciap.domain.catalogue;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ServicePriceVersionRepository extends JpaRepository<ServicePriceVersion, Long> {
    Optional<ServicePriceVersion> findFirstByServiceIdOrderByIdDesc(Long serviceId);
    List<ServicePriceVersion> findByServiceIdOrderByIdDesc(Long serviceId);
}
