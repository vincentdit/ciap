package tz.go.tirdo.ciap.domain.knowledge;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tz.go.tirdo.ciap.common.exception.ResourceNotFoundException;
import tz.go.tirdo.ciap.domain.knowledge.dto.CreateArticleRequest;
import tz.go.tirdo.ciap.security.AccessGuard;

@Service
public class KnowledgeService {

    private final KnowledgeArticleRepository repository;
    private final AccessGuard accessGuard;

    public KnowledgeService(KnowledgeArticleRepository repository, AccessGuard accessGuard) {
        this.repository = repository;
        this.accessGuard = accessGuard;
    }

    @Transactional
    public KnowledgeArticle create(CreateArticleRequest req) {
        KnowledgeArticle a = KnowledgeArticle.builder()
                .title(req.title())
                .category(req.category())
                .summary(req.summary())
                .body(req.body())
                .published(Boolean.TRUE.equals(req.published()))
                .build();
        return repository.save(a);
    }

    @Transactional
    public KnowledgeArticle update(Long id, CreateArticleRequest req) {
        KnowledgeArticle a = getById(id);
        a.setTitle(req.title());
        a.setCategory(req.category());
        a.setSummary(req.summary());
        a.setBody(req.body());
        if (req.published() != null) a.setPublished(req.published());
        return repository.save(a);
    }

    @Transactional
    public KnowledgeArticle setPublished(Long id, boolean published) {
        KnowledgeArticle a = getById(id);
        a.setPublished(published);
        return repository.save(a);
    }

    @Transactional(readOnly = true)
    public KnowledgeArticle getById(Long id) {
        KnowledgeArticle article = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Article", id));
        if (!article.isPublished() && accessGuard.current().kind() == AccessGuard.Kind.CLIENT) {
            throw new ResourceNotFoundException("Article", id);
        }
        return article;
    }

    @Transactional(readOnly = true)
    public Page<KnowledgeArticle> listAll(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<KnowledgeArticle> listPublished(Pageable pageable) {
        return repository.findByPublishedTrue(pageable);
    }
}
