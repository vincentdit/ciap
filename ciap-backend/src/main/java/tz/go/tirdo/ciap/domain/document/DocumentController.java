package tz.go.tirdo.ciap.domain.document;

import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import tz.go.tirdo.ciap.common.dto.ApiResponse;
import tz.go.tirdo.ciap.domain.document.dto.DocumentResponse;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/documents")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    /** Upload a document against a request (multipart/form-data). */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('CLIENT','EXPERT','HOD','MANAGER','ADMIN','FINANCE')")
    public ResponseEntity<ApiResponse<DocumentResponse>> upload(
            @RequestParam("requestId") Long requestId,
            @RequestParam(value = "category", required = false) DocumentCategory category,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam("file") MultipartFile file) {
        Document doc = documentService.store(requestId, category, description, file);
        return ResponseEntity.ok(ApiResponse.ok("Document uploaded", DocumentResponse.from(doc)));
    }

    @GetMapping("/request/{requestId}")
    @PreAuthorize("hasAnyRole('CLIENT','EXPERT','HOD','MANAGER','ADMIN','FINANCE')")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> listForRequest(@PathVariable Long requestId) {
        List<DocumentResponse> docs = documentService.listForRequest(requestId)
                .stream().map(DocumentResponse::from).toList();
        return ResponseEntity.ok(ApiResponse.ok(docs));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('CLIENT','EXPERT','HOD','MANAGER','ADMIN','FINANCE')")
    public ResponseEntity<ApiResponse<DocumentResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(DocumentResponse.from(documentService.getMetadata(id))));
    }

    /** Download the file bytes. */
    @GetMapping("/{id}/download")
    @PreAuthorize("hasAnyRole('CLIENT','EXPERT','HOD','MANAGER','ADMIN','FINANCE')")
    public ResponseEntity<Resource> download(@PathVariable Long id) {
        Document doc = documentService.getMetadata(id);
        Path path = documentService.resolveContent(doc);
        try {
            Resource resource = new InputStreamResource(Files.newInputStream(path));
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, documentService.contentDisposition(doc))
                    .header("X-Content-Type-Options", "nosniff")
                    .contentType(documentService.downloadMediaType(doc))
                    .contentLength(doc.getSizeBytes())
                    .body(resource);
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read document", e);
        }
    }
}
