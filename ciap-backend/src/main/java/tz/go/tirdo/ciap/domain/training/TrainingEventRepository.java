package tz.go.tirdo.ciap.domain.training;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainingEventRepository extends JpaRepository<TrainingEvent, Long> {
    Page<TrainingEvent> findByPublishedTrue(Pageable pageable);
}
