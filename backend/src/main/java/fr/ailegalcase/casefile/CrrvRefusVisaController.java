package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/crrv-refus-visa-analysis")
public class CrrvRefusVisaController {

    private final CrrvRefusVisaService service;

    public CrrvRefusVisaController(CrrvRefusVisaService service) {
        this.service = service;
    }

    @PostMapping
    public CrrvRefusVisaResponse calculate(@PathVariable UUID caseFileId,
                                           @RequestBody CrrvRefusVisaRequest request,
                                           @AuthenticationPrincipal OidcUser oidcUser,
                                           Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public CrrvRefusVisaResponse get(@PathVariable UUID caseFileId,
                                     @AuthenticationPrincipal OidcUser oidcUser,
                                     Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
