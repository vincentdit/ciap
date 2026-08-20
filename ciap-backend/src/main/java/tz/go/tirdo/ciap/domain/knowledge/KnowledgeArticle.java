package tz.go.tirdo.ciap.domain.knowledge;

import jakarta.persistence.*;
import lombok.*;
import tz.go.tirdo.ciap.common.audit.Auditable;

/**
 * A knowledge-base article: industrial guidance, technical notes, FAQs.
 * Authored by staff, optionally published for client visibility.
 */
@Entity
@Table(name = "knowledge_articles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class KnowledgeArticle extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 80)
    private String category;

    @Column(name = "summary", length = 500)
    private String summary;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String body;

    /** When false, the article is a draft visible only to staff. */
    @Column(nullable = false)
    @Builder.Default
    private boolean published = false;
}
