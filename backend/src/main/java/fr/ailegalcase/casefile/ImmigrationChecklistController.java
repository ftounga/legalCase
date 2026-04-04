package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/immigration-checklist")
public class ImmigrationChecklistController {

    private final ImmigrationChecklistService checklistService;

    public ImmigrationChecklistController(ImmigrationChecklistService checklistService) {
        this.checklistService = checklistService;
    }

    @GetMapping
    public ImmigrationChecklistResponse get(@PathVariable UUID caseFileId,
                                            @RequestParam String titreType,
                                            @RequestParam String country,
                                            @AuthenticationPrincipal OidcUser oidcUser,
                                            Principal principal) {
        return checklistService.get(caseFileId, titreType, country, oidcUser, principal);
    }

    @PutMapping
    public ImmigrationChecklistResponse upsert(@PathVariable UUID caseFileId,
                                               @RequestBody ImmigrationChecklistRequest request,
                                               @AuthenticationPrincipal OidcUser oidcUser,
                                               Principal principal) {
        return checklistService.upsert(caseFileId, request, oidcUser, principal);
    }
}
