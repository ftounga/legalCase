package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-218-03 : endpoints POST/GET pour l'analyse de l'exécution forcée d'un
 * jugement CPH (art. 514 CPC ; R. 1454-28 CPC ; L. 3253-6 et s. Code travail).
 * Outil single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/execution-jugement-cph-analysis")
public class ExecutionJugementCphController {

    private final ExecutionJugementCphService service;

    public ExecutionJugementCphController(ExecutionJugementCphService service) {
        this.service = service;
    }

    @PostMapping
    public ExecutionJugementCphResponse analyze(@PathVariable UUID caseFileId,
                                                @RequestBody ExecutionJugementCphRequest request,
                                                @AuthenticationPrincipal OidcUser oidcUser,
                                                Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ExecutionJugementCphResponse get(@PathVariable UUID caseFileId,
                                            @AuthenticationPrincipal OidcUser oidcUser,
                                            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
