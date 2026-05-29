package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-215-13 : endpoints REST de l'outil "Recours CCE annulation 30 jours BE"
 * (art. 39/2 §2 et 39/82 §4 al. 1 Loi 15/12/1980 — Conseil du Contentieux des
 * Étrangers). Outil BELGIQUE UNIQUEMENT (droit des étrangers).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/cce-annulation-be-analysis")
public class CceAnnulationBeController {

    private final CceAnnulationBeService service;

    public CceAnnulationBeController(CceAnnulationBeService service) {
        this.service = service;
    }

    @PostMapping
    public CceAnnulationBeResponse calculate(@PathVariable UUID caseFileId,
                                             @RequestBody CceAnnulationBeRequest request,
                                             @AuthenticationPrincipal OidcUser oidcUser,
                                             Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public CceAnnulationBeResponse get(@PathVariable UUID caseFileId,
                                       @AuthenticationPrincipal OidcUser oidcUser,
                                       Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
