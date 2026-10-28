package tz.go.tirdo.ciap.domain.billing;

import jakarta.persistence.*;
import lombok.*;
import tz.go.tirdo.ciap.common.audit.Auditable;
import tz.go.tirdo.ciap.domain.consultancy.ConsultancyRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quotations")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Quotation extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "quotation_no", nullable = false, unique = true, length = 30)
    private String quotationNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false)
    private ConsultancyRequest request;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private QuotationStatus status = QuotationStatus.DRAFT;

    @Column(nullable = false, length = 3)
    @Builder.Default
    private String currency = "TZS";

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal subtotal = BigDecimal.ZERO;

    @Column(name = "vat_rate", nullable = false, precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal vatRate = new BigDecimal("0.18");

    @Column(name = "vat_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal vatAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal total = BigDecimal.ZERO;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(length = 1000)
    private String notes;

    @Column(name = "version_no", nullable = false)
    @Builder.Default
    private int versionNo = 1;

    /** Previous quotation this one replaces, when a newer version is issued. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supersedes_id")
    private Quotation supersedes;

    @OneToMany(mappedBy = "quotation", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<QuotationItem> items = new ArrayList<>();

    public void addItem(QuotationItem item) {
        item.setQuotation(this);
        this.items.add(item);
    }
}
