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
    private final JurisprudenceReevaluationService reevaluationService;
    private final SuperAdminService superAdminService;

    public JurisprudenceWatchAdminController(JurisprudenceWatchAdminService adminService,
                                             JurisprudenceBootstrapService bootstrapService,
                                             JurisprudenceReevaluationService reevaluationService,
                                             SuperAdminService superAdminService) {
        this.adminService = adminService;
        this.bootstrapService = bootstrapService;
        this.reevaluationService = reevaluationService;
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
    public ResponseEntity<JurisprudenceBootstrapJobStarted> bootstrap(
            @Valid @RequestBody JurisprudenceBootstrapRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        User user = superAdminService.assertSuperAdmin(oidcUser, OAuthProviderResolver.resolve(principal));
        JurisprudenceBootstrapJobStarted started = bootstrapService.startBootstrap(request, user);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(started);
    }

    @GetMapping("/bootstrap/jobs/{jobId}")
    public ResponseEntity<JurisprudenceBootstrapJobStatusResponse> getBootstrapJobStatus(
            @PathVariable UUID jobId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        superAdminService.assertSuperAdmin(oidcUser, OAuthProviderResolver.resolve(principal));
        return bootstrapService.findJob(jobId)
                .map(JurisprudenceBootstrapJobStatusResponse::from)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /**
     * SF-JU-06-02 — ré-évaluation + archivage des mappings existants (SUPER_ADMIN).
     * Passe chaque mapping actif dans les garde-fous qualité (chapeau vide,
     * confiance &lt; 0,70, 2ᵉ passe de pertinence) et archive les non-conformes.
     * Asynchrone : suivre les archivages dans l'audit log. À lancer hors fenêtre
     * de déploiement.
     */
    @PostMapping("/reevaluate")
    public ResponseEntity<JurisprudenceReevaluationService.JurisprudenceReevaluationStarted> reevaluate(
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        User user = superAdminService.assertSuperAdmin(oidcUser, OAuthProviderResolver.resolve(principal));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(reevaluationService.startReevaluation(user));
    }

    /**
     * SF-JU-01-15 — création manuelle d'un mapping ad hoc (SUPER_ADMIN).
     * Cas usage : combler les outils pour lesquels Claude n'a pas trouvé de
     * candidat dans le bootstrap (FR mots-clés génériques, outils BE non
     * couverts par JUDILIBRE en attendant F-JU-04).
     */
    @PostMapping("/mappings")
    public ResponseEntity<ManualMappingCreatedResponse> createManualMapping(
            @Valid @RequestBody ManualMappingCreateRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        User user = superAdminService.assertSuperAdmin(oidcUser, OAuthProviderResolver.resolve(principal));
        ToolJurisprudenceMapping mapping = adminService.createManualMapping(request, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(ManualMappingCreatedResponse.from(mapping));
    }
}
