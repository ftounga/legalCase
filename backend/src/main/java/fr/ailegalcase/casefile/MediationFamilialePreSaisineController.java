package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/mediation-familiale-pre-saisine")
public class MediationFamilialePreSaisineController {

    private final MediationFamilialePreSaisineService service;

    public MediationFamilialePreSaisineController(MediationFamilialePreSaisineService service) {
        this.service = service;
    }

    @PostMapping
    public MediationFamilialePreSaisineResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody MediationFamilialePreSaisineRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public MediationFamilialePreSaisineResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
