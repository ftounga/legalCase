package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/licenciement")
public class LicenciementController {

    private final LicenciementService licenciementService;

    public LicenciementController(LicenciementService licenciementService) {
        this.licenciementService = licenciementService;
    }

    @PostMapping
    public LicenciementResponse analyze(@PathVariable UUID caseFileId,
                                         @RequestBody LicenciementRequest request,
                                         @AuthenticationPrincipal OidcUser oidcUser,
                                         Principal principal) {
        return licenciementService.analyze(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public LicenciementResponse get(@PathVariable UUID caseFileId,
                                     @AuthenticationPrincipal OidcUser oidcUser,
                                     Principal principal) {
        return licenciementService.get(caseFileId, oidcUser, principal);
    }
}
