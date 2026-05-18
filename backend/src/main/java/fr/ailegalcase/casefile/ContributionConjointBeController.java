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
 * SF-217-08 : endpoints REST pour l'outil décisionnel "Pension alimentaire entre
 * ex-époux" (Vague 2 Famille BE — CC art. 301, BELGIQUE).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/contribution-conjoint-be")
public class ContributionConjointBeController {

    private final ContributionConjointBeService service;

    public ContributionConjointBeController(ContributionConjointBeService service) {
        this.service = service;
    }

    @PostMapping
    public ContributionConjointBeResponse calculate(@PathVariable UUID caseFileId,
                                                    @RequestBody ContributionConjointBeRequest request,
                                                    @AuthenticationPrincipal OidcUser oidcUser,
                                                    Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ContributionConjointBeResponse get(@PathVariable UUID caseFileId,
                                              @AuthenticationPrincipal OidcUser oidcUser,
                                              Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
