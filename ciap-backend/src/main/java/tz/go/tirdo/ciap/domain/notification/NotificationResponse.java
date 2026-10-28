package tz.go.tirdo.ciap.domain.notification;

import java.time.Instant;

public record NotificationResponse(
        Long id,
        String message,
        boolean read,
        Instant createdAt
) {
    public static NotificationResponse from(AppNotification notice) {
        return new NotificationResponse(notice.getId(), notice.getMessage(), notice.isReadFlag(), notice.getCreatedAt());
    }
}
