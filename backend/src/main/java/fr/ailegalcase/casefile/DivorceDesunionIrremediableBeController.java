package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-FA-11-01 : endpoint de l'outil "Divorce pour désunion irrémédiable BE"
 * (art. 229 §3 Code civil belge, Loi 27/04/2007). Single-country BELGIQUE.
 * Contrat figé pour SF-FA-11-02 frontend.
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/desunion-irremediable-be")
public class DivorceDesunionIrremediableBeController {

    private final DivorceDesunionIrremediableBeService service;

    public DivorceDesunionIrremediableBeController(DivorceDesunionIrremediableBeService service) {
        this.service = service;
    }

    @PostMapping
    public DivorceDesunionIrremediableBeResponse calculate(@PathVariable UUID caseFileId,
                                                            @RequestBody DivorceDesunionIrremediableBeRequest request,
                                                            @AuthenticationPrincipal OidcUser oidcUser,
                                                            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public DivorceDesunionIrremediableBeResponse get(@PathVariable UUID caseFileId,
                                                      @AuthenticationPrincipal OidcUser oidcUser,
                                                      Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
