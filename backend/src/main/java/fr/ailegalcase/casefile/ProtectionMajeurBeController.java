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
 * SF-217-14 : endpoints REST pour l'analyse de protection du majeur BE
 * (loi 17/03/2013 — statut unique d'administration — BELGIQUE).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/protection-majeur-be")
public class ProtectionMajeurBeController {

    private final ProtectionMajeurBeService service;

    public ProtectionMajeurBeController(ProtectionMajeurBeService service) {
        this.service = service;
    }

    @PostMapping
    public ProtectionMajeurBeResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody ProtectionMajeurBeRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ProtectionMajeurBeResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
