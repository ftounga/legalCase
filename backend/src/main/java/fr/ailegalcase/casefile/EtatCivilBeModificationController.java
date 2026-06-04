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
 * SF-223-09 : endpoints REST pour la modification de l'état civil BE
 * (changement de nom / prénom — loi 18/06/2018 ; changement de sexe — loi
 * 25/06/2017 — à vérifier par avocat belge).
 * POST/GET /api/v1/case-files/{caseFileId}/etat-civil-be-modification-analysis
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/etat-civil-be-modification-analysis")
public class EtatCivilBeModificationController {

    private final EtatCivilBeModificationService service;

    public EtatCivilBeModificationController(EtatCivilBeModificationService service) {
        this.service = service;
    }

    @PostMapping
    public EtatCivilBeModificationResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody EtatCivilBeModificationRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public EtatCivilBeModificationResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
