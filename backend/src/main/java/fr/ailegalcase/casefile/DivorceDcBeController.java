package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/divorce-dc-be-analysis")
public class DivorceDcBeController {

    private final DivorceDcBeService service;

    public DivorceDcBeController(DivorceDcBeService service) {
        this.service = service;
    }

    @PostMapping
    public DivorceDcBeResponse calculate(@PathVariable UUID caseFileId,
                                         @RequestBody DivorceDcBeRequest request,
                                         @AuthenticationPrincipal OidcUser oidcUser,
                                         Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public DivorceDcBeResponse get(@PathVariable UUID caseFileId,
                                   @AuthenticationPrincipal OidcUser oidcUser,
                                   Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
