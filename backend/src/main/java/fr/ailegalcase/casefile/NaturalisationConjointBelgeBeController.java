package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-215-09 : endpoints REST de l'outil naturalisation conjoint Belge BE
 * (Code de la nationalité belge art. 16 — déclaration par mariage avec un(e) Belge).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/naturalisation-conjoint-belge-be-analysis")
public class NaturalisationConjointBelgeBeController {

    private final NaturalisationConjointBelgeBeService service;

    public NaturalisationConjointBelgeBeController(NaturalisationConjointBelgeBeService service) {
        this.service = service;
    }

    @PostMapping
    public NaturalisationConjointBelgeBeResponse calculate(@PathVariable UUID caseFileId,
                                                           @RequestBody NaturalisationConjointBelgeBeRequest request,
                                                           @AuthenticationPrincipal OidcUser oidcUser,
                                                           Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public NaturalisationConjointBelgeBeResponse get(@PathVariable UUID caseFileId,
                                                     @AuthenticationPrincipal OidcUser oidcUser,
                                                     Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
