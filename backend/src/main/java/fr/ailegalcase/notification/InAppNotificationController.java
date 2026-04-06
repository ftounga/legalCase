package fr.ailegalcase.notification;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notifications")
public class InAppNotificationController {

    private final InAppNotificationService notificationService;

    public InAppNotificationController(InAppNotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @GetMapping
    public Page<InAppNotificationResponse> getNotifications(
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return notificationService.getNotifications(oidcUser, principal, pageable);
    }

    @GetMapping("/unread-count")
    public UnreadCountResponse getUnreadCount(
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return notificationService.getUnreadCount(oidcUser, principal);
    }

    @PatchMapping("/{id}/read")
    public void markAsRead(
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal,
            @PathVariable UUID id) {
        notificationService.markAsRead(oidcUser, principal, id);
    }

    @PatchMapping("/read-all")
    public void markAllAsRead(
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        notificationService.markAllAsRead(oidcUser, principal);
    }
}
