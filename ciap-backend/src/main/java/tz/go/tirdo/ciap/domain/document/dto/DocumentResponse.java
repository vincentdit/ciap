package tz.go.tirdo.ciap.domain.document.dto;

import tz.go.tirdo.ciap.domain.document.Document;
import tz.go.tirdo.ciap.domain.document.DocumentCategory;

import java.time.Instant;

public record DocumentResponse(
        Long id,
        Long requestId,
        String requestReference,
        DocumentCategory category,
        String originalFilename,
        String contentType,
        long sizeBytes,
        String description,
        String uploadedBy,
        Instant uploadedAt
) {
    public static DocumentResponse from(Document d) {
        return new DocumentResponse(
                d.getId(),
                d.getRequest().getId(),
                d.getRequest().getReferenceNo(),
                d.getCategory(),
                d.getOriginalFilename(),
                d.getContentType(),
                d.getSizeBytes(),
                d.getDescription(),
                d.getCreatedBy(),
                d.getCreatedAt()
        );
    }
}
