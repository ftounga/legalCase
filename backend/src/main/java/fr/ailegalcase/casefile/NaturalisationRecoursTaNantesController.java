package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-214-31 : endpoints POST/GET pour l'analyse du délai de recours devant le
 * Tribunal administratif de Nantes contre un refus de naturalisation par décret
 * (CJA L. 213-1, délai 2 mois ; Cciv 21-15). Outil single-country FR.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/naturalisation-recours-ta-analysis")
public class NaturalisationRecoursTaNantesController {

    private final NaturalisationRecoursTaNantesService service;

    public NaturalisationRecoursTaNantesController(NaturalisationRecoursTaNantesService service) {
        this.service = service;
    }

    @PostMapping
    public NaturalisationRecoursTaNantesResponse analyze(@PathVariable UUID caseFileId,
                                                         @RequestBody NaturalisationRecoursTaNantesRequest request,
                                                         @AuthenticationPrincipal OidcUser oidcUser,
                                                         Principal principal) {
        return service.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public NaturalisationRecoursTaNantesResponse get(@PathVariable UUID caseFileId,
                                                     @AuthenticationPrincipal OidcUser oidcUser,
                                                     Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
