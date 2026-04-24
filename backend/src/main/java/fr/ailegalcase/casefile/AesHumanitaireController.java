package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/aes-humanitaire")
public class AesHumanitaireController {

    private final AesHumanitaireService service;

    public AesHumanitaireController(AesHumanitaireService service) {
        this.service = service;
    }

    @PostMapping
    public AesHumanitaireResponse calculate(@PathVariable UUID caseFileId,
                                            @RequestBody AesHumanitaireRequest request,
                                            @AuthenticationPrincipal OidcUser oidcUser,
                                            Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public AesHumanitaireResponse get(@PathVariable UUID caseFileId,
                                      @AuthenticationPrincipal OidcUser oidcUser,
                                      Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
