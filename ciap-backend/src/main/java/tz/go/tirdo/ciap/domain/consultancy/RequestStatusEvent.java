package tz.go.tirdo.ciap.domain.consultancy;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "request_status_events", indexes = {
        @Index(name = "idx_req_events_request", columnList = "request_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RequestStatusEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_id", nullable = false)
    private Long requestId;

    @Column(name = "from_status", length = 40)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 40)
    private String toStatus;

    @Column(nullable = false, length = 150)
    private String actor;

    @Column(length = 500)
    private String reason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
