package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/annexe13-be")
public class Annexe13BeController {

    private final Annexe13BeService service;

    public Annexe13BeController(Annexe13BeService service) {
        this.service = service;
    }

    @PostMapping
    public Annexe13BeResponse calculate(@PathVariable UUID caseFileId,
                                        @RequestBody Annexe13BeRequest request,
                                        @AuthenticationPrincipal OidcUser oidcUser,
                                        Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public Annexe13BeResponse get(@PathVariable UUID caseFileId,
                                  @AuthenticationPrincipal OidcUser oidcUser,
                                  Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
