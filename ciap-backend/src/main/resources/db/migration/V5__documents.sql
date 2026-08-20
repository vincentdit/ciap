-- =============================================================
-- CIAP — Document Management (Module 11)
-- Document metadata linked to a consultancy request. File bytes
-- are stored on the filesystem under the configured storage root.
-- =============================================================

CREATE TABLE documents (
    id                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    request_id         BIGINT       NOT NULL,
    category           VARCHAR(20)  NOT NULL,
    original_filename  VARCHAR(255) NOT NULL,
    stored_filename    VARCHAR(255) NOT NULL,
    content_type       VARCHAR(120),
    size_bytes         BIGINT       NOT NULL,
    description        VARCHAR(500),
    created_at         DATETIME(6)  NOT NULL,
    updated_at         DATETIME(6),
    created_by         VARCHAR(100),
    updated_by         VARCHAR(100),
    CONSTRAINT uq_document_stored UNIQUE (stored_filename),
    CONSTRAINT fk_document_request FOREIGN KEY (request_id) REFERENCES consultancy_requests(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE INDEX idx_document_request ON documents(request_id);
