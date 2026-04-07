package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/immigration/recours")
public class ImmigrationRecoursController {

    private final ImmigrationRecoursService recoursService;

    public ImmigrationRecoursController(ImmigrationRecoursService recoursService) {
        this.recoursService = recoursService;
    }

    @PostMapping
    public ImmigrationRecoursResponse generate(@PathVariable UUID caseFileId,
                                                @RequestBody ImmigrationRecoursRequest request,
                                                @AuthenticationPrincipal OidcUser oidcUser,
                                                Principal principal) {
        return recoursService.generate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public ImmigrationRecoursResponse get(@PathVariable UUID caseFileId,
                                           @AuthenticationPrincipal OidcUser oidcUser,
                                           Principal principal) {
        return recoursService.get(caseFileId, oidcUser, principal);
    }
}
