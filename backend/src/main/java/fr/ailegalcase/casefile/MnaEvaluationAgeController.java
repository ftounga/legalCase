package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-214-27 : endpoints POST/GET pour l'analyse de la procédure d'évaluation
 * d'âge MNA (F-IM-38-mna-evaluation-age-fr). Outil single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/mna-evaluation-age-analysis")
public class MnaEvaluationAgeController {

    private final MnaEvaluationAgeService service;

    public MnaEvaluationAgeController(MnaEvaluationAgeService service) {
        this.service = service;
    }

    @PostMapping
    public MnaEvaluationAgeResponse analyze(@PathVariable UUID caseFileId,
                                            @RequestBody MnaEvaluationAgeRequest request,
                                            @AuthenticationPrincipal OidcUser oidcUser,
                                            Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public MnaEvaluationAgeResponse get(@PathVariable UUID caseFileId,
                                        @AuthenticationPrincipal OidcUser oidcUser,
                                        Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
