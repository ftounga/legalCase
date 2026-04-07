package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/calendrier-garde")
public class CalendrierGardeController {
    private final CalendrierGardeService service;
    public CalendrierGardeController(CalendrierGardeService service) { this.service = service; }

    @PostMapping
    public CalendrierGardeResponse generate(@PathVariable UUID caseFileId, @RequestBody CalendrierGardeRequest request,
                                             @AuthenticationPrincipal OidcUser oidcUser, Principal principal) {
        return service.generate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public CalendrierGardeResponse get(@PathVariable UUID caseFileId,
                                        @AuthenticationPrincipal OidcUser oidcUser, Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
