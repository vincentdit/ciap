package tz.go.tirdo.ciap.domain.training;

import jakarta.persistence.*;
import lombok.*;
import tz.go.tirdo.ciap.common.audit.Auditable;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * A training session or webinar offered by TIRDO. Clients can register;
 * staff create and publish events.
 */
@Entity
@Table(name = "training_events")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TrainingEvent extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TrainingMode mode;

    /** Venue (in-person) or joining link/details (online). */
    @Column(length = 300)
    private String location;

    @Column(name = "starts_at", nullable = false)
    private Instant startsAt;

    @Column(name = "ends_at")
    private Instant endsAt;

    /** 0 = unlimited. */
    @Column(nullable = false)
    @Builder.Default
    private int capacity = 0;

    @Column(nullable = false)
    @Builder.Default
    private boolean published = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_kind", nullable = false, length = 10)
    @Builder.Default
    private FeeKind feeKind = FeeKind.FREE;

    @Column(name = "fee_amount", precision = 15, scale = 2)
    private BigDecimal feeAmount;

    @Column(name = "fee_currency", nullable = false, length = 3)
    @Builder.Default
    private String feeCurrency = "TZS";

    @Column(name = "registration_opens_at")
    private Instant registrationOpensAt;

    @Column(name = "registration_closes_at")
    private Instant registrationClosesAt;

    @Column(length = 1000)
    private String objectives;

    @Column(length = 500)
    private String audience;

    @Column(length = 500)
    private String prerequisites;

    @Column(name = "contact_email", length = 160)
    private String contactEmail;
}
