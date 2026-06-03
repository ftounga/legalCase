package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-222-03 : endpoint REST pour l'outil habilitation familiale (art. 494-1 et
 * s. Cciv — F-FA-HABILITATION-FAMILIALE, FRANCE).
 * POST/GET /api/v1/case-files/{caseFileId}/habilitation-familiale-analysis
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/habilitation-familiale-analysis")
public class HabilitationFamilialeController {

    private final HabilitationFamilialeService service;

    public HabilitationFamilialeController(HabilitationFamilialeService service) {
        this.service = service;
    }

    @PostMapping
    public HabilitationFamilialeResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody HabilitationFamilialeRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public HabilitationFamilialeResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
