package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-221-05 : endpoints REST de l'outil recours CCE en suspension ordinaire BE
 * (art. 39/82 Loi 15/12/1980 ; loi 15/09/2006).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/cce-suspension-be-analysis")
public class CceSuspensionBeController {

    private final CceSuspensionBeService service;

    public CceSuspensionBeController(CceSuspensionBeService service) {
        this.service = service;
    }

    @PostMapping
    public CceSuspensionBeResponse calculate(@PathVariable UUID caseFileId,
                                             @RequestBody CceSuspensionBeRequest request,
                                             @AuthenticationPrincipal OidcUser oidcUser,
                                             Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public CceSuspensionBeResponse get(@PathVariable UUID caseFileId,
                                       @AuthenticationPrincipal OidcUser oidcUser,
                                       Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
