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
 * F-284 — Échéancier procédural proactif (lecture seule).
 * Agrège délais (F-69) + échéances de réponse contradictoires (F-282) pour l'onglet Suivi.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/echeancier")
public class EcheancierController {

    private final EcheancierService echeancierService;

    public EcheancierController(EcheancierService echeancierService) {
        this.echeancierService = echeancierService;
    }

    @GetMapping
    public EcheancierResponse get(@PathVariable UUID caseFileId,
                                  @AuthenticationPrincipal OidcUser oidcUser,
                                  Principal principal) {
        return echeancierService.forCaseFile(caseFileId, oidcUser, principal);
    }
}
