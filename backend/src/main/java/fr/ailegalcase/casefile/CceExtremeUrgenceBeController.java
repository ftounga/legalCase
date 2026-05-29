package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-215-15 : endpoints REST de l'outil "Recours CCE extrême urgence 5j BE"
 * (art. 39/82 §4 al. 2-3 Loi 15/12/1980 — Conseil du Contentieux des Étrangers,
 * 5 jours ouvrables). Outil BELGIQUE UNIQUEMENT (droit des étrangers).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/cce-extreme-urgence-be-analysis")
public class CceExtremeUrgenceBeController {

    private final CceExtremeUrgenceBeService service;

    public CceExtremeUrgenceBeController(CceExtremeUrgenceBeService service) {
        this.service = service;
    }

    @PostMapping
    public CceExtremeUrgenceBeResponse calculate(@PathVariable UUID caseFileId,
                                                 @RequestBody CceExtremeUrgenceBeRequest request,
                                                 @AuthenticationPrincipal OidcUser oidcUser,
                                                 Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public CceExtremeUrgenceBeResponse get(@PathVariable UUID caseFileId,
                                           @AuthenticationPrincipal OidcUser oidcUser,
                                           Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
