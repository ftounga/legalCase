package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/tribunal-famille-be-mesures-provisoires-analysis")
public class TribunalFamilleBeMesuresProvisoiresController {

    private final TribunalFamilleBeMesuresProvisoiresService service;

    public TribunalFamilleBeMesuresProvisoiresController(TribunalFamilleBeMesuresProvisoiresService service) {
        this.service = service;
    }

    @PostMapping
    public TribunalFamilleBeMesuresProvisoiresResponse calculate(
            @PathVariable UUID caseFileId,
            @RequestBody TribunalFamilleBeMesuresProvisoiresRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public TribunalFamilleBeMesuresProvisoiresResponse get(
            @PathVariable UUID caseFileId,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
