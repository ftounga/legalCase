package fr.ailegalcase.casefile;

import fr.ailegalcase.shared.OAuthProviderResolver;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

/**
 * F-243 / SF-243-01 — Endpoints REST du stade procédural du dossier.
 *
 * <p>Contrat API figé (importé par SF-243-02) :
 * <ul>
 *   <li><b>A</b> — {@code GET /api/v1/procedure-stage/options?domain=&country=} : référentiel des valeurs.</li>
 *   <li><b>B</b> — {@code GET /api/v1/case-files/{id}/procedure-stage} : lecture du stade d'un dossier.</li>
 *   <li><b>C</b> — {@code PATCH /api/v1/case-files/{id}/procedure-stage} : mise à jour du stade.</li>
 * </ul>
 */
@RestController
public class ProcedureStageController {

    private final ProcedureStageService procedureStageService;

    public ProcedureStageController(ProcedureStageService procedureStageService) {
        this.procedureStageService = procedureStageService;
    }

    /** Endpoint A — référentiel des valeurs valides pour un domaine + pays. */
    @GetMapping("/api/v1/procedure-stage/options")
    public ProcedureStageOptionsResponse getOptions(@RequestParam("domain") String domain,
                                                    @RequestParam("country") String country) {
        return procedureStageService.getOptions(domain, country);
    }

    /** Endpoint B — lecture du stade procédural d'un dossier. */
    @GetMapping("/api/v1/case-files/{id}/procedure-stage")
    public ProcedureStageResponse getProcedureStage(@PathVariable UUID id,
                                                    @AuthenticationPrincipal OidcUser oidcUser,
                                                    Principal principal) {
        return procedureStageService.getProcedureStage(
                id, oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }

    /** Endpoint C — mise à jour du stade procédural d'un dossier. */
    @PatchMapping("/api/v1/case-files/{id}/procedure-stage")
    public ProcedureStageResponse updateProcedureStage(@PathVariable UUID id,
                                                       @Valid @RequestBody ProcedureStageUpdateRequest request,
                                                       @AuthenticationPrincipal OidcUser oidcUser,
                                                       Principal principal) {
        return procedureStageService.updateProcedureStage(
                id, request, oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }
}
