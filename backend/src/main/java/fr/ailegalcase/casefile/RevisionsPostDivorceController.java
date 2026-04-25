package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-FA-13-01 : controller exposant l'outil de révisions post-divorce FR
 * (art. 209 / 373-2-13 / 373-2-9 / 279 / 373-2 Cciv).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/revisions-post-divorce")
public class RevisionsPostDivorceController {

    private final RevisionsPostDivorceService service;

    public RevisionsPostDivorceController(RevisionsPostDivorceService service) {
        this.service = service;
    }

    @PostMapping
    public RevisionsPostDivorceResponse calculate(@PathVariable UUID caseFileId,
                                                  @RequestBody RevisionsPostDivorceRequest request,
                                                  @AuthenticationPrincipal OidcUser oidcUser,
                                                  Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public RevisionsPostDivorceResponse get(@PathVariable UUID caseFileId,
                                            @AuthenticationPrincipal OidcUser oidcUser,
                                            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
