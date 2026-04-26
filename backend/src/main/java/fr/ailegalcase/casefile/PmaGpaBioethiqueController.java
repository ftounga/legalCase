package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-FA-27-01 : controller HTTP pour l'analyse PMA / GPA / bioéthique
 * (loi 2/8/2021 + Cass. 18/12/2022).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/pma-gpa-bioethique")
public class PmaGpaBioethiqueController {

    private final PmaGpaBioethiqueService service;

    public PmaGpaBioethiqueController(PmaGpaBioethiqueService service) {
        this.service = service;
    }

    @PostMapping
    public PmaGpaBioethiqueResponse calculate(@PathVariable UUID caseFileId,
                                              @RequestBody PmaGpaBioethiqueRequest request,
                                              @AuthenticationPrincipal OidcUser oidcUser,
                                              Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public PmaGpaBioethiqueResponse get(@PathVariable UUID caseFileId,
                                        @AuthenticationPrincipal OidcUser oidcUser,
                                        Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
