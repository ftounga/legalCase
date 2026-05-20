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
 * SF-217-12 : endpoints REST pour l'analyse d'option successorale BE
 * (acceptation pure / sous bénéfice d'inventaire / renonciation — BELGIQUE).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/succession-be-acceptation-renonciation")
public class SuccessionBeAcceptationRenonciationController {

    private final SuccessionBeAcceptationRenonciationService service;

    public SuccessionBeAcceptationRenonciationController(
            SuccessionBeAcceptationRenonciationService service) {
        this.service = service;
    }

    @PostMapping
    public SuccessionBeAcceptationRenonciationResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody SuccessionBeAcceptationRenonciationRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public SuccessionBeAcceptationRenonciationResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
