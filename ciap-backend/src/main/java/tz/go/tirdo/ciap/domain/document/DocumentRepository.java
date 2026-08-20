package tz.go.tirdo.ciap.domain.document;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {
    List<Document> findByRequestIdOrderByCreatedAtDesc(Long requestId);
    Optional<Document> findByStoredFilename(String storedFilename);
}
