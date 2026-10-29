package tz.go.tirdo.ciap.domain.catalogue;

import jakarta.persistence.*;
import lombok.*;
import tz.go.tirdo.ciap.common.audit.Auditable;

import java.math.BigDecimal;

@Entity
@Table(name = "consultancy_services", uniqueConstraints = @UniqueConstraint(
        name = "uq_service_category_name", columnNames = {"category_id", "name"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ServiceOffering extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private ConsultancyCategory category;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_method", nullable = false, length = 20)
    private PricingMethod pricingMethod;

    @Column(precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "TZS";

    @Column(name = "unit_label", length = 80)
    private String unitLabel;

    @Column(length = 1000)
    private String conditions;

    @Column(nullable = false)
    @Builder.Default
    private boolean published = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
