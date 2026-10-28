package tz.go.tirdo.ciap.domain.document;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;
import tz.go.tirdo.ciap.common.exception.BusinessException;
import tz.go.tirdo.ciap.common.exception.ResourceNotFoundException;
import tz.go.tirdo.ciap.domain.consultancy.ConsultancyRequest;
import tz.go.tirdo.ciap.domain.consultancy.ConsultancyRequestRepository;
import tz.go.tirdo.ciap.security.AccessGuard;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
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
    private static final long MAX_BYTES = 25L * 1024 * 1024;

    private static final Map<String, String> ALLOWED_TYPES = Map.ofEntries(
            Map.entry("pdf", MediaType.APPLICATION_PDF_VALUE),
            Map.entry("png", MediaType.IMAGE_PNG_VALUE),
            Map.entry("jpg", MediaType.IMAGE_JPEG_VALUE),
            Map.entry("jpeg", MediaType.IMAGE_JPEG_VALUE),
            Map.entry("gif", MediaType.IMAGE_GIF_VALUE),
            Map.entry("webp", "image/webp"),
            Map.entry("txt", MediaType.TEXT_PLAIN_VALUE),
            Map.entry("csv", "text/csv"),
            Map.entry("doc", "application/msword"),
            Map.entry("docx", "application/vnd.openxmlformats-officedocument.wordprocessingml.document"),
            Map.entry("xls", "application/vnd.ms-excel"),
            Map.entry("xlsx", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
            Map.entry("ppt", "application/vnd.ms-powerpoint"),
            Map.entry("pptx", "application/vnd.openxmlformats-officedocument.presentationml.presentation"),
            Map.entry("odt", "application/vnd.oasis.opendocument.text"),
            Map.entry("ods", "application/vnd.oasis.opendocument.spreadsheet")
    );

    private final DocumentRepository documentRepository;
    private final ConsultancyRequestRepository requestRepository;
    private final AccessGuard accessGuard;
    private final Path storageRoot;

    public DocumentService(DocumentRepository documentRepository,
                           ConsultancyRequestRepository requestRepository,
                           StorageProperties props,
                           AccessGuard accessGuard) {
        this.documentRepository = documentRepository;
        this.requestRepository = requestRepository;
        this.accessGuard = accessGuard;
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
            throw new BusinessException("File is empty");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BusinessException("File exceeds the 25 MB limit");
        }
        ConsultancyRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("ConsultancyRequest", requestId));
        accessGuard.assertCanReadRequest(request);

        String original = sanitize(file.getOriginalFilename());
        String ext = extensionOf(original);
        if (!ALLOWED_TYPES.containsKey(ext)) {
            throw new BusinessException("This file type is not allowed");
        }
        if (!signatureMatches(ext, file)) {
            throw new BusinessException("File content does not match its extension");
        }
        if (description != null && description.length() > 500) {
            throw new BusinessException("Description is too long");
        }
        String stored = UUID.randomUUID() + (ext.isEmpty() ? "" : "." + ext);
        Path target = storageRoot.resolve(stored).normalize();

        // Guard against path traversal: target must remain under the root.
        if (!target.startsWith(storageRoot)) {
            throw new BusinessException("Invalid storage path");
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
                .contentType(ALLOWED_TYPES.get(ext))
                .sizeBytes(file.getSize())
                .description(description)
                .build();
        return documentRepository.save(doc);
    }

    @Transactional(readOnly = true)
    public List<Document> listForRequest(Long requestId) {
        ConsultancyRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("ConsultancyRequest", requestId));
        accessGuard.assertCanReadRequest(request);
        List<Document> documents = documentRepository.findByRequestIdOrderByCreatedAtDesc(requestId);
        documents.forEach(document -> document.getRequest().getReferenceNo());
        return documents;
    }

    @Transactional(readOnly = true)
    public Document getMetadata(Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", id));
        accessGuard.assertCanReadRequest(document.getRequest());
        return document;
    }

    /**
     * PDF, PNG, and JPEG must start with their real file signature.
     * Other allowed types have no reliable signature and are left to the extension allowlist.
     */
    private boolean signatureMatches(String ext, MultipartFile file) {
        if (!ext.equals("pdf") && !ext.equals("png") && !ext.equals("jpg") && !ext.equals("jpeg")) {
            return true;
        }
        byte[] head = new byte[8];
        int read;
        try (InputStream in = file.getInputStream()) {
            read = in.read(head);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read file", e);
        }
        if (read < 0) return false;
        return switch (ext) {
            case "pdf" -> read >= 4 && head[0] == '%' && head[1] == 'P' && head[2] == 'D' && head[3] == 'F';
            case "png" -> read >= 8
                    && (head[0] & 0xff) == 0x89 && head[1] == 0x50 && head[2] == 0x4E && head[3] == 0x47
                    && head[4] == 0x0D && head[5] == 0x0A && head[6] == 0x1A && head[7] == 0x0A;
            case "jpg", "jpeg" -> read >= 3
                    && (head[0] & 0xff) == 0xFF && (head[1] & 0xff) == 0xD8 && (head[2] & 0xff) == 0xFF;
            default -> true;
        };
    }

    /** Media type derived from the stored extension, never from the client header. */
    public MediaType downloadMediaType(Document doc) {
        String type = ALLOWED_TYPES.get(extensionOf(doc.getStoredFilename()));
        return MediaType.parseMediaType(type != null ? type : MediaType.APPLICATION_OCTET_STREAM_VALUE);
    }

    /** Attachment disposition that cannot break out of the header. */
    public String contentDisposition(Document doc) {
        String cleaned = doc.getOriginalFilename() == null ? "download" : doc.getOriginalFilename();
        cleaned = cleaned.replaceAll("[\\r\\n\"]", "");
        String ascii = cleaned.replaceAll("[^A-Za-z0-9._ -]", "_");
        if (ascii.isBlank()) {
            ascii = "download";
        }
        return "attachment; filename=\"" + ascii + "\"";
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
        String ext = name.substring(dot + 1).toLowerCase();
        return ext.matches("[a-z0-9]{1,10}") ? ext : "";
    }
}
