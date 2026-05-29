package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-214-15 : endpoints POST/GET pour l'analyse récépissé vs attestation de
 * prolongation R. 311-4 / R. 311-6 CESEDA. Outil single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/recepisse-attestation-analysis")
public class RecepisseAttestationController {

    private final RecepisseAttestationService service;

    public RecepisseAttestationController(RecepisseAttestationService service) {
        this.service = service;
    }

    @PostMapping
    public RecepisseAttestationResponse analyze(@PathVariable UUID caseFileId,
                                                @RequestBody RecepisseAttestationRequest request,
                                                @AuthenticationPrincipal OidcUser oidcUser,
                                                Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public RecepisseAttestationResponse get(@PathVariable UUID caseFileId,
                                            @AuthenticationPrincipal OidcUser oidcUser,
                                            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
