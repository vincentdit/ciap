package tz.go.tirdo.ciap.domain.consultancy;

import jakarta.persistence.*;
import lombok.*;
import tz.go.tirdo.ciap.common.audit.Auditable;
import tz.go.tirdo.ciap.domain.catalogue.PricingMethod;
import tz.go.tirdo.ciap.domain.client.Client;
import tz.go.tirdo.ciap.domain.consultant.Consultant;
import tz.go.tirdo.ciap.domain.department.Department;
import tz.go.tirdo.ciap.domain.department.RoutingStatus;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "consultancy_requests", indexes = {
        @Index(name = "idx_req_reference", columnList = "reference_no", unique = true),
        @Index(name = "idx_req_status", columnList = "status")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ConsultancyRequest extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Human-readable reference, e.g. CIAP-2026-000123. */
    @Column(name = "reference_no", nullable = false, unique = true, length = 30)
    private String referenceNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Enumerated(EnumType.STRING)
    @Column(name = "consultancy_type", nullable = false, length = 30)
    private ConsultancyType consultancyType;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "expected_start_date")
    private LocalDate expectedStartDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private RequestStatus status = RequestStatus.SUBMITTED;

    /** Consultant assigned at the ASSIGNED step. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_consultant_id")
    private Consultant assignedConsultant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Enumerated(EnumType.STRING)
    @Column(name = "routing_status", nullable = false, length = 20)
    @Builder.Default
    private RoutingStatus routingStatus = RoutingStatus.UNMAPPED;

    @Column(name = "assignment_deadline")
    private LocalDate assignmentDeadline;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    /** Service the customer selected. The price fields are a copy of what they confirmed. */
    @Column(name = "service_id")
    private Long serviceId;

    @Column(name = "service_name", length = 200)
    private String serviceName;

    @Enumerated(EnumType.STRING)
    @Column(name = "pricing_method", length = 20)
    private PricingMethod pricingMethod;

    @Column(name = "snapshot_amount", precision = 15, scale = 2)
    private BigDecimal snapshotAmount;

    @Column(name = "snapshot_currency", length = 3)
    private String snapshotCurrency;

    @Column(name = "price_version_id")
    private Long priceVersionId;

    @Column(name = "quoted_quantity", precision = 12, scale = 2)
    private BigDecimal quotedQuantity;

    @Version
    @Column(name = "lock_version", nullable = false)
    @Builder.Default
    private long lockVersion = 0;
}
