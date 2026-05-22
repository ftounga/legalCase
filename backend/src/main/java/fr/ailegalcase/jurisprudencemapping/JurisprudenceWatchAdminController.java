package fr.ailegalcase.jurisprudencemapping;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.shared.OAuthProviderResolver;
import fr.ailegalcase.superadmin.SuperAdminService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

/**
 * F-JU-01 / SF-JU-01-05 — endpoints super-admin pour le dashboard
 * {@code /super-admin/jurisprudence-watch}.
 *
 * <p>Tous les endpoints sont gated par {@link SuperAdminService#assertSuperAdmin}.</p>
 */
@RestController
@RequestMapping("/api/v1/super-admin/jurisprudence-watch")
public class JurisprudenceWatchAdminController {

    private final JurisprudenceWatchAdminService adminService;
    private final JurisprudenceBootstrapService bootstrapService;
    private final SuperAdminService superAdminService;

    public JurisprudenceWatchAdminController(JurisprudenceWatchAdminService adminService,
                                             JurisprudenceBootstrapService bootstrapService,
                                             SuperAdminService superAdminService) {
        this.adminService = adminService;
        this.bootstrapService = bootstrapService;
        this.superAdminService = superAdminService;
    }

    @GetMapping("/flags")
    public Page<JurisprudenceWatchFlagResponse> listFlags(
            @RequestParam(name = "statut", required = false) JurisprudenceWatchFlagStatut statut,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        superAdminService.assertSuperAdmin(oidcUser, OAuthProviderResolver.resolve(principal));
        return adminService.listFlags(statut, page, size);
    }

    @PostMapping("/flags/{flagId}/arbitrate")
    public JurisprudenceWatchFlagResponse arbitrate(
            @PathVariable UUID flagId,
            @Valid @RequestBody JurisprudenceArbitrateRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        User user = superAdminService.assertSuperAdmin(oidcUser, OAuthProviderResolver.resolve(principal));
        return adminService.arbitrate(flagId, request, user);
    }

    @GetMapping("/audit-log")
    public Page<JurisprudenceAuditLogResponse> listAuditLog(
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "50") int size,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        superAdminService.assertSuperAdmin(oidcUser, OAuthProviderResolver.resolve(principal));
        return adminService.listAuditLog(page, size);
    }

    @PostMapping("/bootstrap")
    public ResponseEntity<JurisprudenceBootstrapResponse> bootstrap(
            @Valid @RequestBody JurisprudenceBootstrapRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        User user = superAdminService.assertSuperAdmin(oidcUser, OAuthProviderResolver.resolve(principal));
        JurisprudenceBootstrapResponse response = bootstrapService.runBootstrap(request, user);
        return ResponseEntity.status(HttpStatus.OK).body(response);
    }
}
