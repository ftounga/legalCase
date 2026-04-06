package fr.ailegalcase.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface InAppNotificationRepository extends JpaRepository<InAppNotification, UUID> {

    Page<InAppNotification> findByUserIdAndWorkspaceIdOrderByCreatedAtDesc(
            UUID userId, UUID workspaceId, Pageable pageable);

    long countByUserIdAndWorkspaceIdAndIsReadFalse(UUID userId, UUID workspaceId);

    Optional<InAppNotification> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    @Modifying
    @Query("UPDATE InAppNotification n SET n.isRead = true, n.readAt = :now " +
           "WHERE n.userId = :userId AND n.workspaceId = :workspaceId AND n.isRead = false")
    int markAllAsRead(UUID userId, UUID workspaceId, Instant now);
}
