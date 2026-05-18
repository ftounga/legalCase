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
 * SF-217-06 : endpoints REST pour l'outil décisionnel "Contribution alimentaire
 * des enfants" (Vague 2 Famille BE — méthode Renard, BELGIQUE).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/contribution-alimentaire-enfants-be")
public class ContributionAlimentaireEnfantsBeController {

    private final ContributionAlimentaireEnfantsBeService service;

    public ContributionAlimentaireEnfantsBeController(ContributionAlimentaireEnfantsBeService service) {
        this.service = service;
    }

    @PostMapping
    public ContributionAlimentaireEnfantsBeResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody ContributionAlimentaireEnfantsBeRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ContributionAlimentaireEnfantsBeResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
