package tz.go.tirdo.ciap.domain.knowledge.dto;

import tz.go.tirdo.ciap.domain.knowledge.KnowledgeArticle;

import java.time.Instant;

public record ArticleResponse(
        Long id,
        String title,
        String category,
        String summary,
        String body,
        boolean published,
        String author,
        Instant createdAt,
        Instant updatedAt
) {
    public static ArticleResponse from(KnowledgeArticle a) {
        return new ArticleResponse(
                a.getId(), a.getTitle(), a.getCategory(), a.getSummary(), a.getBody(),
                a.isPublished(), a.getCreatedBy(), a.getCreatedAt(), a.getUpdatedAt());
    }

    /** List view without the full body. */
    public static ArticleResponse summary(KnowledgeArticle a) {
        return new ArticleResponse(
                a.getId(), a.getTitle(), a.getCategory(), a.getSummary(), null,
                a.isPublished(), a.getCreatedBy(), a.getCreatedAt(), a.getUpdatedAt());
    }
}
