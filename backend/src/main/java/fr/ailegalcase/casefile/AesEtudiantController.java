package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/aes-etudiant")
public class AesEtudiantController {

    private final AesEtudiantService service;

    public AesEtudiantController(AesEtudiantService service) {
        this.service = service;
    }

    @PostMapping
    public AesEtudiantResponse calculate(@PathVariable UUID caseFileId,
                                         @RequestBody AesEtudiantRequest request,
                                         @AuthenticationPrincipal OidcUser oidcUser,
                                         Principal principal) {
        return service.calculate(caseFileId, request, oidcUser, principal);
    }

    @GetMapping
    public AesEtudiantResponse get(@PathVariable UUID caseFileId,
                                   @AuthenticationPrincipal OidcUser oidcUser,
                                   Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }
}
