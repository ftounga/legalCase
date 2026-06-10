package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

/**
 * F-262 SF-262-01 — Endpoint REST du référentiel des chefs de demande.
 *
 * <p>{@code GET /api/v1/case-files/{caseFileId}/heads-of-claim} retourne les
 * chefs de demande <strong>applicables</strong> au dossier (filet de
 * complétude). Lecture seule ; isolation workspace assurée par le service
 * (404 cross-workspace).</p>
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/heads-of-claim")
public class HeadsOfClaimController {

    private final HeadsOfClaimService service;

    public HeadsOfClaimController(HeadsOfClaimService service) {
        this.service = service;
    }

    @GetMapping
    public HeadsOfClaimResponse getHeadsOfClaim(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.detect(caseFileId, oidcUser, principal);
    }
}
