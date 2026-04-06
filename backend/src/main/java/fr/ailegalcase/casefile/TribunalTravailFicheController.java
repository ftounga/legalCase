package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/tribunal-travail-fiche")
@Validated
public class TribunalTravailFicheController {

    private final TribunalTravailFicheService ficheService;

    public TribunalTravailFicheController(TribunalTravailFicheService ficheService) {
        this.ficheService = ficheService;
    }

    @GetMapping
    public TribunalTravailFicheResponse get(@PathVariable UUID caseFileId,
                                             @AuthenticationPrincipal OidcUser oidcUser,
                                             Principal principal) {
        return ficheService.get(caseFileId, oidcUser, principal);
    }

    @PutMapping
    public TribunalTravailFicheResponse upsert(@PathVariable UUID caseFileId,
                                                @RequestBody @jakarta.validation.Valid TribunalTravailFicheRequest request,
                                                @AuthenticationPrincipal OidcUser oidcUser,
                                                Principal principal) {
        return ficheService.upsert(caseFileId, request, oidcUser, principal);
    }
}
