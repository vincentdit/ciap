package tz.go.tirdo.ciap.domain.training;

import jakarta.persistence.*;
import lombok.*;
import tz.go.tirdo.ciap.common.audit.Auditable;
import tz.go.tirdo.ciap.domain.billing.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;

/** Payment for a training registration. It is not a consultancy-request payment. */
@Entity
@Table(name = "training_payments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrainingPayment extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "control_number", nullable = false, unique = true, length = 20)
    private String controlNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registration_id", nullable = false)
    private TrainingRegistration registration;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "gepg_reference", length = 50)
    private String gepgReference;

    @Column(name = "paid_at")
    private Instant paidAt;
}
