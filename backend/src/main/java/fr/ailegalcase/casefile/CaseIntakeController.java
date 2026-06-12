package fr.ailegalcase.casefile;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * F-285 / SF-285-01 — endpoints de la qualification d'entrée (intake) d'un dossier.
 * Isolation workspace via le dossier (cf. {@link CaseIntakeService}).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/intake")
@Validated
public class CaseIntakeController {

    private final CaseIntakeService service;

    public CaseIntakeController(CaseIntakeService service) {
        this.service = service;
    }

    @GetMapping
    public CaseIntakeResponse get(@PathVariable UUID caseFileId,
                                  @AuthenticationPrincipal OidcUser oidcUser,
                                  Principal principal) {
        return service.get(caseFileId, oidcUser, principal);
    }

    @PutMapping
    public CaseIntakeResponse update(@PathVariable UUID caseFileId,
                                     @RequestBody @jakarta.validation.Valid CaseIntakeRequest request,
                                     @AuthenticationPrincipal OidcUser oidcUser,
                                     Principal principal) {
        return service.update(caseFileId, request, oidcUser, principal);
    }
}
