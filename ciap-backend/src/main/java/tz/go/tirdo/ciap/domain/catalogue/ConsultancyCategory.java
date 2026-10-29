package tz.go.tirdo.ciap.domain.catalogue;

import jakarta.persistence.*;
import lombok.*;
import tz.go.tirdo.ciap.common.audit.Auditable;
import tz.go.tirdo.ciap.domain.consultancy.ConsultancyType;

@Entity
@Table(name = "consultancy_categories")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ConsultancyCategory extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String code;

    @Column(nullable = false, unique = true, length = 120)
    private String name;

    @Column(length = 1000)
    private String description;

    /** Department service line used when a request for this category is routed. */
    @Enumerated(EnumType.STRING)
    @Column(name = "consultancy_type", nullable = false, length = 30)
    private ConsultancyType consultancyType;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private int displayOrder = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
