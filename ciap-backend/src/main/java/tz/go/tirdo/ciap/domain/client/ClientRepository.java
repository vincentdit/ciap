package tz.go.tirdo.ciap.domain.client;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ClientRepository extends JpaRepository<Client, Long> {
    boolean existsByTin(String tin);
    Optional<Client> findByUserId(Long userId);
    Page<Client> findByIndustryCategory(IndustryCategory category, Pageable pageable);
}
