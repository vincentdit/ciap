package tz.go.tirdo.ciap.domain.training;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TrainingEventRepository extends JpaRepository<TrainingEvent, Long> {
    Page<TrainingEvent> findByPublishedTrue(Pageable pageable);

    @Query("""
            select e from TrainingEvent e
            where e.published = true
              and (:q is null
                   or lower(e.title) like :q
                   or lower(coalesce(e.description, '')) like :q
                   or lower(coalesce(e.location, '')) like :q)
            """)
    Page<TrainingEvent> searchPublished(@Param("q") String q, Pageable pageable);

    @Query("""
            select e from TrainingEvent e
            where :q is null
               or lower(e.title) like :q
               or lower(coalesce(e.description, '')) like :q
               or lower(coalesce(e.location, '')) like :q
            """)
    Page<TrainingEvent> searchAll(@Param("q") String q, Pageable pageable);
}
