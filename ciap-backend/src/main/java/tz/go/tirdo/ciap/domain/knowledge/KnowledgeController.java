package tz.go.tirdo.ciap.domain.knowledge;

import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import tz.go.tirdo.ciap.common.dto.ApiResponse;
import tz.go.tirdo.ciap.common.dto.PageResponse;
import tz.go.tirdo.ciap.domain.knowledge.dto.ArticleResponse;
import tz.go.tirdo.ciap.domain.knowledge.dto.CreateArticleRequest;

@RestController
@RequestMapping("/knowledge")
public class KnowledgeController {

    private final KnowledgeService service;

    public KnowledgeController(KnowledgeService service) {
        this.service = service;
    }

    /** Published articles — visible to any authenticated user. */
    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<ArticleResponse>>> listPublished(Pageable pageable) {
        var page = service.listPublished(pageable).map(ArticleResponse::summary);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(page)));
    }

    /** All articles including drafts — staff only. */
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EXPERT')")
    public ResponseEntity<ApiResponse<PageResponse<ArticleResponse>>> listAll(Pageable pageable) {
        var page = service.listAll(pageable).map(ArticleResponse::summary);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(page)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ArticleResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(ArticleResponse.from(service.getById(id))));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EXPERT')")
    public ResponseEntity<ApiResponse<ArticleResponse>> create(@Valid @RequestBody CreateArticleRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Article created", ArticleResponse.from(service.create(req))));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EXPERT')")
    public ResponseEntity<ApiResponse<ArticleResponse>> update(@PathVariable Long id,
                                                               @Valid @RequestBody CreateArticleRequest req) {
        return ResponseEntity.ok(ApiResponse.ok("Article updated", ArticleResponse.from(service.update(id, req))));
    }

    @PostMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<ArticleResponse>> publish(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(ArticleResponse.from(service.setPublished(id, true))));
    }

    @PostMapping("/{id}/unpublish")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<ApiResponse<ArticleResponse>> unpublish(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(ArticleResponse.from(service.setPublished(id, false))));
    }
}
