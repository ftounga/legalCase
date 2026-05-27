package fr.ailegalcase.notification;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.Instant;
import java.util.UUID;

import static fr.ailegalcase.shared.OAuthProviderResolver.resolve;

@Service
public class InAppNotificationService {

    // SF-113-04 — limites alignées sur les colonnes de la migration 052-create-in-app-notifications.xml.
    // Toute valeur dépassant est tronquée avec ellipsis avant persist (défense en profondeur appliquée
    // dans create() pour couvrir les 5+ callers existants en un seul point — cf. HF-2026-05-27-02).
    static final int TITLE_MAX = 255;
    static final int MESSAGE_MAX = 1000;
    static final int LINK_MAX = 500;
    private static final String ELLIPSIS = "…";

    private final InAppNotificationRepository notificationRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;

    public InAppNotificationService(InAppNotificationRepository notificationRepository,
                                     WorkspaceMemberRepository workspaceMemberRepository,
                                     CurrentUserResolver currentUserResolver) {
        this.notificationRepository = notificationRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
    }

    /**
     * Creates a notification for a specific user in a workspace.
     * Called internally by event listeners — not exposed via REST.
     * SF-113-04 : tronque title/message/link à la longueur max de la colonne DB
     * pour éviter les DataIntegrityViolationException sur des labels longs.
     */
    @Transactional
    public InAppNotification create(UUID userId, UUID workspaceId, NotificationType type,
                                     String title, String message, String link) {
        InAppNotification notification = new InAppNotification();
        notification.setUserId(userId);
        notification.setWorkspaceId(workspaceId);
        notification.setType(type);
        notification.setTitle(truncateWithEllipsis(title, TITLE_MAX));
        notification.setMessage(truncateWithEllipsis(message, MESSAGE_MAX));
        notification.setLink(truncateWithEllipsis(link, LINK_MAX));
        return notificationRepository.save(notification);
    }

    /**
     * Tronque la valeur à maxLen caractères en remplaçant le dernier caractère
     * par une ellipsis si la chaîne dépasse — préserve la lisibilité et indique
     * visuellement la troncation à l'utilisateur.
     */
    private static String truncateWithEllipsis(String value, int maxLen) {
        if (value == null || value.length() <= maxLen) return value;
        return value.substring(0, maxLen - 1) + ELLIPSIS;
    }

    @Transactional(readOnly = true)
    public Page<InAppNotificationResponse> getNotifications(OidcUser oidcUser, Principal principal,
                                                             Pageable pageable) {
        WorkspaceMember member = resolveWorkspaceMember(oidcUser, principal);
        return notificationRepository
                .findByUserIdAndWorkspaceIdOrderByCreatedAtDesc(
                        member.getUser().getId(),
                        member.getWorkspace().getId(),
                        pageable)
                .map(InAppNotificationResponse::from);
    }

    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount(OidcUser oidcUser, Principal principal) {
        WorkspaceMember member = resolveWorkspaceMember(oidcUser, principal);
        long count = notificationRepository.countByUserIdAndWorkspaceIdAndIsReadFalse(
                member.getUser().getId(),
                member.getWorkspace().getId());
        return new UnreadCountResponse(count);
    }

    @Transactional
    public void markAsRead(OidcUser oidcUser, Principal principal, UUID notificationId) {
        WorkspaceMember member = resolveWorkspaceMember(oidcUser, principal);
        UUID workspaceId = member.getWorkspace().getId();

        InAppNotification notification = notificationRepository.findByIdAndWorkspaceId(notificationId, workspaceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Notification not found"));

        if (!notification.getUserId().equals(member.getUser().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
            notificationRepository.save(notification);
        }
    }

    @Transactional
    public void markAllAsRead(OidcUser oidcUser, Principal principal) {
        WorkspaceMember member = resolveWorkspaceMember(oidcUser, principal);
        notificationRepository.markAllAsRead(
                member.getUser().getId(),
                member.getWorkspace().getId(),
                Instant.now());
    }

    private WorkspaceMember resolveWorkspaceMember(OidcUser oidcUser, Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, resolve(principal), principal);
        return workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"));
    }
}
