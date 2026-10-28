package tz.go.tirdo.ciap.domain.consultant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ConsultantRepository extends JpaRepository<Consultant, Long> {
    Page<Consultant> findByCategory(ExpertCategory category, Pageable pageable);
    List<Consultant> findByCategoryAndAvailabilityStatus(ExpertCategory category, AvailabilityStatus status);
    java.util.Optional<Consultant> findByUser_Id(Long userId);

    @Query("""
            select c from Consultant c
            where :q is null
               or lower(c.fullName) like :q
               or lower(coalesce(c.skills, '')) like :q
            """)
    Page<Consultant> search(@Param("q") String q, Pageable pageable);
}
