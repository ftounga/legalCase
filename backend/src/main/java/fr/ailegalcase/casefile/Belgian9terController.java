package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/belgian-9ter")
public class Belgian9terController {

    private final Belgian9terService service;

    public Belgian9terController(Belgian9terService service) {
        this.service = service;
    }

    @PostMapping
    public Belgian9terResponse calculate(@PathVariable UUID caseFileId,
                                         @RequestBody Belgian9terRequest request,
                                         @AuthenticationPrincipal OidcUser oidcUser,
                                         Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public Belgian9terResponse get(@PathVariable UUID caseFileId,
                                   @AuthenticationPrincipal OidcUser oidcUser,
                                   Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
