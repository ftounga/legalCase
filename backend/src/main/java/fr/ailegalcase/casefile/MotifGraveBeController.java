package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/motif-grave-be")
public class MotifGraveBeController {

    private final MotifGraveBeService service;

    public MotifGraveBeController(MotifGraveBeService service) {
        this.service = service;
    }

    @PostMapping
    public MotifGraveBeResponse calculate(@PathVariable UUID caseFileId,
                                          @RequestBody MotifGraveBeRequest request,
                                          @AuthenticationPrincipal OidcUser oidcUser,
                                          Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public MotifGraveBeResponse get(@PathVariable UUID caseFileId,
                                    @AuthenticationPrincipal OidcUser oidcUser,
                                    Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
