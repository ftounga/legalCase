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
 * SF-223-08 : endpoints REST pour la reconnaissance / exequatur d'une décision
 * familiale étrangère BE (CDIP art. 22-27 ; art. 21 Const. / CC art. 161 — à
 * vérifier par avocat belge).
 * POST/GET /api/v1/case-files/{caseFileId}/dip-be-reconnaissance-decision-etrangere-analysis
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/dip-be-reconnaissance-decision-etrangere-analysis")
public class DipBeReconnaissanceDecisionEtrangereController {

    private final DipBeReconnaissanceDecisionEtrangereService service;

    public DipBeReconnaissanceDecisionEtrangereController(
            DipBeReconnaissanceDecisionEtrangereService service) {
        this.service = service;
    }

    @PostMapping
    public DipBeReconnaissanceDecisionEtrangereResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody DipBeReconnaissanceDecisionEtrangereRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public DipBeReconnaissanceDecisionEtrangereResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
