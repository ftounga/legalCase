package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/victime-violences-l4256-analysis")
public class VictimeViolencesL4256Controller {

    private final VictimeViolencesL4256Service service;

    public VictimeViolencesL4256Controller(VictimeViolencesL4256Service service) {
        this.service = service;
    }

    @PostMapping
    public VictimeViolencesL4256Response calculate(@PathVariable UUID caseFileId,
                                                   @RequestBody VictimeViolencesL4256Request request,
                                                   @AuthenticationPrincipal OidcUser oidcUser,
                                                   Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public VictimeViolencesL4256Response get(@PathVariable UUID caseFileId,
                                             @AuthenticationPrincipal OidcUser oidcUser,
                                             Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
