package tz.go.tirdo.ciap.domain.consultant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ConsultantRepository extends JpaRepository<Consultant, Long> {
    Page<Consultant> findByCategory(ExpertCategory category, Pageable pageable);
    List<Consultant> findByCategoryAndAvailabilityStatus(ExpertCategory category, AvailabilityStatus status);
}
