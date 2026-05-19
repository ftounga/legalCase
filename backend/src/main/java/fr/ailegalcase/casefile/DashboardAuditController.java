package fr.ailegalcase.casefile;

import fr.ailegalcase.casefile.DashboardAuditDtos.DashboardAuditReport;
import fr.ailegalcase.shared.OAuthProviderResolver;
import fr.ailegalcase.superadmin.SuperAdminService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

/**
 * F-180 SF-180-01 — endpoints super-admin de l'audit dashboard tiles F-167.
 *
 * <p>Les deux endpoints sont gardés par {@code SuperAdminService.assertSuperAdmin}
 * (403 si l'appelant n'est pas super-admin) — même pattern que
 * {@code BacklogController}.</p>
 */
@RestController
@RequestMapping("/api/v1/super-admin/dashboard-audit")
public class DashboardAuditController {

    private final DashboardAuditService auditService;
    private final SuperAdminService superAdminService;

    public DashboardAuditController(DashboardAuditService auditService,
                                    SuperAdminService superAdminService) {
        this.auditService = auditService;
        this.superAdminService = superAdminService;
    }

    /** Dernier rapport d'audit. Déclenche un run si aucun n'existe encore. */
    @GetMapping("/latest")
    public DashboardAuditReport getLatest(@AuthenticationPrincipal OidcUser oidcUser,
                                          Principal principal) {
        superAdminService.assertSuperAdmin(oidcUser, OAuthProviderResolver.resolve(principal));
        return auditService.getLatest();
    }

    /** Force un audit immédiat (bouton « Relancer maintenant »). */
    @PostMapping("/run")
    public DashboardAuditReport run(@AuthenticationPrincipal OidcUser oidcUser,
                                    Principal principal) {
        superAdminService.assertSuperAdmin(oidcUser, OAuthProviderResolver.resolve(principal));
        return auditService.runAudit();
    }
}
