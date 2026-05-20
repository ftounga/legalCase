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
 * SF-217-11 : endpoints REST pour l'outil décisionnel "Succession BE —
 * dévolution légale et réserve héréditaire post-2017" (Vague 3 Famille BE —
 * CC art. 913 nouveau, 745bis, 740, 1477 — tous taggés `(à vérifier)`).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/succession-be-devolution-reserve")
public class SuccessionBeDevolutionReserveController {

    private final SuccessionBeDevolutionReserveService service;

    public SuccessionBeDevolutionReserveController(SuccessionBeDevolutionReserveService service) {
        this.service = service;
    }

    @PostMapping
    public SuccessionBeDevolutionReserveResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody SuccessionBeDevolutionReserveRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public SuccessionBeDevolutionReserveResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
