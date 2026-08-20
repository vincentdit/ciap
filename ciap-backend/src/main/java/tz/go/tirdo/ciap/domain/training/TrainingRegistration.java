package tz.go.tirdo.ciap.domain.training;

import jakarta.persistence.*;
import lombok.*;
import tz.go.tirdo.ciap.common.audit.Auditable;

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
}
