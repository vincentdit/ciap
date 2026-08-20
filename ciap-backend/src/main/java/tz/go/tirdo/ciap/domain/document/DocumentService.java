package tz.go.tirdo.ciap.domain.document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import tz.go.tirdo.ciap.common.exception.ResourceNotFoundException;
import tz.go.tirdo.ciap.domain.consultancy.ConsultancyRequest;
import tz.go.tirdo.ciap.domain.consultancy.ConsultancyRequestRepository;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

/**
 * Stores document metadata in the DB and file bytes on the filesystem under a
 * configured root directory. The stored filename is an opaque UUID to avoid
 * collisions and path-traversal via the original name.
 */
@Service
@EnableConfigurationProperties(StorageProperties.class)
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final DocumentRepository documentRepository;
    private final ConsultancyRequestRepository requestRepository;
    private final Path storageRoot;

    public DocumentService(DocumentRepository documentRepository,
                           ConsultancyRequestRepository requestRepository,
                           StorageProperties props) {
        this.documentRepository = documentRepository;
        this.requestRepository = requestRepository;
        this.storageRoot = Paths.get(props.root()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(storageRoot);
            log.info("Document storage root: {}", storageRoot);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create document storage root: " + storageRoot, e);
        }
    }

    @Transactional
    public Document store(Long requestId, DocumentCategory category, String description, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }
        ConsultancyRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("ConsultancyRequest", requestId));

        String original = sanitize(file.getOriginalFilename());
        String ext = extensionOf(original);
        String stored = UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
        Path target = storageRoot.resolve(stored).normalize();

        // Guard against path traversal: target must remain under the root.
        if (!target.startsWith(storageRoot)) {
            throw new IllegalArgumentException("Invalid storage path");
        }

        try {
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to store file", e);
        }

        Document doc = Document.builder()
                .request(request)
                .category(category != null ? category : DocumentCategory.OTHER)
                .originalFilename(original)
                .storedFilename(stored)
                .contentType(file.getContentType())
                .sizeBytes(file.getSize())
                .description(description)
                .build();
        return documentRepository.save(doc);
    }

    @Transactional(readOnly = true)
    public List<Document> listForRequest(Long requestId) {
        return documentRepository.findByRequestIdOrderByCreatedAtDesc(requestId);
    }

    @Transactional(readOnly = true)
    public Document getMetadata(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", id));
    }

    /** Resolve the on-disk path for a document, verifying it stays under the root. */
    @Transactional(readOnly = true)
    public Path resolveContent(Document doc) {
        Path p = storageRoot.resolve(doc.getStoredFilename()).normalize();
        if (!p.startsWith(storageRoot) || !Files.exists(p)) {
            throw new ResourceNotFoundException("Document file", doc.getId());
        }
        return p;
    }

    private String sanitize(String name) {
        if (name == null || name.isBlank()) return "upload";
        // Strip any path components a client may have sent.
        String base = Paths.get(name).getFileName().toString();
        return base.length() > 255 ? base.substring(base.length() - 255) : base;
    }

    private String extensionOf(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) return "";
        String ext = name.substring(dot + 1);
        return ext.matches("[A-Za-z0-9]{1,10}") ? ext : "";
    }
}
