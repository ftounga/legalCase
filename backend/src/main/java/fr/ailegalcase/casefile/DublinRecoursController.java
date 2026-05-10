package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/dublin-recours-analysis")
public class DublinRecoursController {

    private final DublinRecoursService service;

    public DublinRecoursController(DublinRecoursService service) {
        this.service = service;
    }

    @PostMapping
    public DublinRecoursResponse calculate(@PathVariable UUID caseFileId,
                                           @RequestBody DublinRecoursRequest request,
                                           @AuthenticationPrincipal OidcUser oidcUser,
                                           Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public DublinRecoursResponse get(@PathVariable UUID caseFileId,
                                     @AuthenticationPrincipal OidcUser oidcUser,
                                     Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
