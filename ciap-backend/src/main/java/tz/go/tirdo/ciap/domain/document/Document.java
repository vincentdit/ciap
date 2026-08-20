package tz.go.tirdo.ciap.domain.document;

import jakarta.persistence.*;
import lombok.*;
import tz.go.tirdo.ciap.common.audit.Auditable;
import tz.go.tirdo.ciap.domain.consultancy.ConsultancyRequest;

/**
 * Metadata for a stored document. The file bytes live on the filesystem under
 * a configured storage root; this row holds everything else and links the
 * document to a consultancy request.
 */
@Entity
@Table(name = "documents")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Document extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The request this document belongs to. */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private ConsultancyRequest request;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentCategory category;

    /** Original filename as uploaded. */
    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    /** Stored filename (opaque, unique) on disk. */
    @Column(name = "stored_filename", nullable = false, unique = true, length = 255)
    private String storedFilename;

    @Column(name = "content_type", length = 120)
    private String contentType;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(length = 500)
    private String description;
}
