package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-223-06 : endpoints REST pour la qualification du régime de séparation de
 * biens BE (Livre 3 CC ; loi du 22/07/2018 — à vérifier par avocat belge).
 * POST/GET /api/v1/case-files/{caseFileId}/regime-be-separation-biens-analysis
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/regime-be-separation-biens-analysis")
public class RegimeBeSeparationBiensController {

    private final RegimeBeSeparationBiensService service;

    public RegimeBeSeparationBiensController(RegimeBeSeparationBiensService service) {
        this.service = service;
    }

    @PostMapping
    public RegimeBeSeparationBiensResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody RegimeBeSeparationBiensRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public RegimeBeSeparationBiensResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
