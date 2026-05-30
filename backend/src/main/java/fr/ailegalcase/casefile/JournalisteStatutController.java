package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-218-15 : endpoints POST/GET pour l'analyse du statut de journaliste
 * professionnel lors d'une rupture — clause de cession / conscience, indemnité
 * de congédiement, commission arbitrale (F-DT-105). Outil single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/journaliste-statut-analysis")
public class JournalisteStatutController {

    private final JournalisteStatutService service;

    public JournalisteStatutController(JournalisteStatutService service) {
        this.service = service;
    }

    @PostMapping
    public JournalisteStatutResponse analyze(@PathVariable UUID caseFileId,
                                             @RequestBody JournalisteStatutRequest request,
                                             @AuthenticationPrincipal OidcUser oidcUser,
                                             Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public JournalisteStatutResponse get(@PathVariable UUID caseFileId,
                                         @AuthenticationPrincipal OidcUser oidcUser,
                                         Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
