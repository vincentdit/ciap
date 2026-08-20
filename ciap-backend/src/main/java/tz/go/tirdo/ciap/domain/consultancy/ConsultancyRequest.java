package tz.go.tirdo.ciap.domain.consultancy;

import jakarta.persistence.*;
import lombok.*;
import tz.go.tirdo.ciap.common.audit.Auditable;
import tz.go.tirdo.ciap.domain.client.Client;
import tz.go.tirdo.ciap.domain.consultant.Consultant;

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

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;
}
