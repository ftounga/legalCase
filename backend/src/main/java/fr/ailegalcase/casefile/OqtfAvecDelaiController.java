package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/oqtf-avec-delai")
public class OqtfAvecDelaiController {

    private final OqtfAvecDelaiService service;

    public OqtfAvecDelaiController(OqtfAvecDelaiService service) {
        this.service = service;
    }

    @PostMapping
    public OqtfAvecDelaiResponse calculate(@PathVariable UUID caseFileId,
                                           @RequestBody OqtfAvecDelaiRequest request,
                                           @AuthenticationPrincipal OidcUser oidcUser,
                                           Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public OqtfAvecDelaiResponse get(@PathVariable UUID caseFileId,
                                     @AuthenticationPrincipal OidcUser oidcUser,
                                     Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
