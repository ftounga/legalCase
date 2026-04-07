package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/partage-immobilier")
public class PartageImmobilierController {

    private final PartageImmobilierService service;

    public PartageImmobilierController(PartageImmobilierService service) {
        this.service = service;
    }

    @PostMapping
    public PartageImmobilierResponse calculate(@PathVariable UUID caseFileId,
                                                @RequestBody PartageImmobilierRequest request,
                                                @AuthenticationPrincipal OidcUser oidcUser,
                                                Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public PartageImmobilierResponse get(@PathVariable UUID caseFileId,
                                          @AuthenticationPrincipal OidcUser oidcUser,
                                          Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
