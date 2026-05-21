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
 * SF-217-18 : endpoints REST pour l'analyse de recevabilité d'une contestation
 * de filiation BE (paternité présumée du mari / paternité reconnue volontairement —
 * BELGIQUE).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/contestation-filiation-be")
public class ContestationFiliationBeController {

    private final ContestationFiliationBeService service;

    public ContestationFiliationBeController(ContestationFiliationBeService service) {
        this.service = service;
    }

    @PostMapping
    public ContestationFiliationBeResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody ContestationFiliationBeRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ContestationFiliationBeResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
