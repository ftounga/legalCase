package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-215-11 : endpoints REST de l'outil composite AESM + tutelle MENA BE —
 * F-IM-30-aesm-mena-be (Loi 04/05/2007 tutelle MENA + loi 15/12/1980 art. 9bis
 * adapté MENA + circulaire OE 15/09/2005).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/aesm-mena-be-analysis")
public class AesmMenaBeController {

    private final AesmMenaBeService service;

    public AesmMenaBeController(AesmMenaBeService service) {
        this.service = service;
    }

    @PostMapping
    public AesmMenaBeResponse calculate(@PathVariable UUID caseFileId,
                                        @RequestBody AesmMenaBeRequest request,
                                        @AuthenticationPrincipal OidcUser oidcUser,
                                        Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public AesmMenaBeResponse get(@PathVariable UUID caseFileId,
                                  @AuthenticationPrincipal OidcUser oidcUser,
                                  Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
