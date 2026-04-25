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
 * SF-DT-24-01 : endpoints REST pour l'analyse de validité d'une clause de
 * non-concurrence (FR — Cass. soc. 10/07/2002 + art. L.1221-1).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/non-concurrence")
public class NonConcurrenceController {

    private final NonConcurrenceService service;

    public NonConcurrenceController(NonConcurrenceService service) {
        this.service = service;
    }

    @PostMapping
    public NonConcurrenceResponse calculate(@PathVariable UUID caseFileId,
                                            @RequestBody NonConcurrenceRequest request,
                                            @AuthenticationPrincipal OidcUser oidcUser,
                                            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public NonConcurrenceResponse get(@PathVariable UUID caseFileId,
                                      @AuthenticationPrincipal OidcUser oidcUser,
                                      Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
