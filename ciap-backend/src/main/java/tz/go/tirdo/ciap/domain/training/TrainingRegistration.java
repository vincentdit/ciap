package tz.go.tirdo.ciap.domain.training;

import jakarta.persistence.*;
import lombok.*;
import tz.go.tirdo.ciap.common.audit.Auditable;

import java.math.BigDecimal;

/**
 * A registration of a user (by email) for a training event. The email comes
 * from the authenticated principal at registration time.
 */
@Entity
@Table(name = "training_registrations",
       uniqueConstraints = @UniqueConstraint(name = "uq_reg_event_email", columnNames = {"event_id", "registrant_email"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrainingRegistration extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private TrainingEvent event;

    @Column(name = "registrant_email", nullable = false, length = 160)
    private String registrantEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_kind", nullable = false, length = 10)
    @Builder.Default
    private FeeKind feeKind = FeeKind.FREE;

    /** Fee copied at registration. Later changes to the event do not alter this. */
    @Column(name = "fee_amount", nullable = false, precision = 15, scale = 2)
    @Builder.Default
    private BigDecimal feeAmount = BigDecimal.ZERO;

    @Column(name = "fee_currency", nullable = false, length = 3)
    @Builder.Default
    private String feeCurrency = "TZS";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private RegistrationStatus status = RegistrationStatus.CONFIRMED;
}
