package tz.go.tirdo.ciap.domain.catalogue;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "service_price_versions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ServicePriceVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "service_id", nullable = false)
    private ServiceOffering service;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_method", nullable = false, length = 20)
    private PricingMethod pricingMethod;

    @Column(precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "unit_label", length = 80)
    private String unitLabel;

    @Column(length = 1000)
    private String conditions;

    @Column(length = 500)
    private String note;

    @Column(name = "changed_by", nullable = false, length = 100)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;
}
