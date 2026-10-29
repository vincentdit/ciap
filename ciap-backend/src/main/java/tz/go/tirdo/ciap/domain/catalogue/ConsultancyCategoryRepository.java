package tz.go.tirdo.ciap.domain.catalogue;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ConsultancyCategoryRepository extends JpaRepository<ConsultancyCategory, Long> {
    Optional<ConsultancyCategory> findByCodeIgnoreCase(String code);
    boolean existsByNameIgnoreCase(String name);
    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);
    List<ConsultancyCategory> findByActiveTrueOrderByDisplayOrderAscNameAsc();
    List<ConsultancyCategory> findAllByOrderByDisplayOrderAscNameAsc();
}
