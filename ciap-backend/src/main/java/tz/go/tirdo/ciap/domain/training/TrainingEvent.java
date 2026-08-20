package tz.go.tirdo.ciap.domain.training;

import jakarta.persistence.*;
import lombok.*;
import tz.go.tirdo.ciap.common.audit.Auditable;

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
}
