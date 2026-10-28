package tz.go.tirdo.ciap.domain.notification;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "app_notifications", indexes = {
        @Index(name = "idx_notifications_email", columnList = "recipient_email")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AppNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient_email", nullable = false, length = 150)
    private String recipientEmail;

    @Column(nullable = false, length = 500)
    private String message;

    @Column(name = "read_flag", nullable = false)
    @Builder.Default
    private boolean readFlag = false;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
