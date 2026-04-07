package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/divorce-checklist")
public class DivorceChecklistController {
    private final DivorceChecklistService service;
    public DivorceChecklistController(DivorceChecklistService service) { this.service = service; }

    @PostMapping
    public DivorceChecklistResponse save(@PathVariable UUID caseFileId, @RequestBody DivorceChecklistRequest request,
                                          @AuthenticationPrincipal OidcUser oidcUser, Principal principal) {
        return service.save(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public DivorceChecklistResponse get(@PathVariable UUID caseFileId,
                                         @AuthenticationPrincipal OidcUser oidcUser, Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
