package tz.go.tirdo.ciap.domain.knowledge;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KnowledgeArticleRepository extends JpaRepository<KnowledgeArticle, Long> {
    Page<KnowledgeArticle> findByPublishedTrue(Pageable pageable);

    @Query("""
            select a from KnowledgeArticle a
            where a.published = true
              and (:q is null
                   or lower(a.title) like :q
                   or lower(coalesce(a.summary, '')) like :q
                   or lower(coalesce(a.category, '')) like :q)
            """)
    Page<KnowledgeArticle> searchPublished(@Param("q") String q, Pageable pageable);

    @Query("""
            select a from KnowledgeArticle a
            where :q is null
               or lower(a.title) like :q
               or lower(coalesce(a.summary, '')) like :q
               or lower(coalesce(a.category, '')) like :q
            """)
    Page<KnowledgeArticle> searchAll(@Param("q") String q, Pageable pageable);
}
