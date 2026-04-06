package fr.ailegalcase.notification;

import java.time.Instant;
import java.util.UUID;

public record InAppNotificationResponse(
        UUID id,
        NotificationType type,
        String title,
        String message,
        String link,
        boolean isRead,
        Instant createdAt,
        Instant readAt
) {
    public static InAppNotificationResponse from(InAppNotification n) {
        return new InAppNotificationResponse(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getMessage(),
                n.getLink(),
                n.isRead(),
                n.getCreatedAt(),
                n.getReadAt()
        );
    }
}
