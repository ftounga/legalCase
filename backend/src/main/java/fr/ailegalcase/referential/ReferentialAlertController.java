package fr.ailegalcase.referential;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.shared.OAuthProviderResolver;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/referentials/alerts")
public class ReferentialAlertController {

    private static final Set<String> OWNER_ADMIN_ROLES = Set.of("OWNER", "ADMIN");

    private final ReferentialAlertService alertService;
    private final CurrentUserResolver currentUserResolver;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public ReferentialAlertController(ReferentialAlertService alertService,
                                      CurrentUserResolver currentUserResolver,
                                      WorkspaceMemberRepository workspaceMemberRepository) {
        this.alertService = alertService;
        this.currentUserResolver = currentUserResolver;
        this.workspaceMemberRepository = workspaceMemberRepository;
    }

    @GetMapping("/pending-count")
    public ResponseEntity<Map<String, Long>> pendingCount(
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        // Any authenticated member can read the count
        currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);
        return ResponseEntity.ok(Map.of("count", alertService.pendingCount()));
    }

    @PostMapping("/{alertId}/apply")
    public ResponseEntity<Void> apply(
            @PathVariable UUID alertId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);
        WorkspaceMember member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Workspace introuvable"));
        if (!OWNER_ADMIN_ROLES.contains(member.getMemberRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seuls les OWNER et ADMIN peuvent appliquer une alerte.");
        }
        alertService.apply(alertId, member.getWorkspace().getId(), user.getId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{alertId}/dismiss")
    public ResponseEntity<Void> dismiss(
            @PathVariable UUID alertId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);
        WorkspaceMember member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Workspace introuvable"));
        if (!OWNER_ADMIN_ROLES.contains(member.getMemberRole())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Seuls les OWNER et ADMIN peuvent ignorer une alerte.");
        }
        alertService.dismiss(alertId, user.getId());
        return ResponseEntity.ok().build();
    }
}
