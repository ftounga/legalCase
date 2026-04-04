package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/prudhome-fiche")
@Validated
public class PrudhomeFicheController {

    private final PrudhomeFicheService ficheService;

    public PrudhomeFicheController(PrudhomeFicheService ficheService) {
        this.ficheService = ficheService;
    }

    @GetMapping
    public PrudhomeFicheResponse get(@PathVariable UUID caseFileId,
                                     @AuthenticationPrincipal OidcUser oidcUser,
                                     Principal principal) {
        return ficheService.get(caseFileId, oidcUser, principal);
    }

    @PutMapping
    public PrudhomeFicheResponse upsert(@PathVariable UUID caseFileId,
                                        @RequestBody @jakarta.validation.Valid PrudhomeFicheRequest request,
                                        @AuthenticationPrincipal OidcUser oidcUser,
                                        Principal principal) {
        return ficheService.upsert(caseFileId, request, oidcUser, principal);
    }
}
