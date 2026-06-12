package fr.ailegalcase.casefile;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.UUID;

/**
 * F-282 / SF-282-01 — endpoints du cycle contradictoire (rounds) d'un dossier.
 * Isolation workspace via le dossier (cf. {@link ContradictoireService}).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/contradictoire-rounds")
@Validated
public class ContradictoireController {

    private final ContradictoireService service;

    public ContradictoireController(ContradictoireService service) {
        this.service = service;
    }

    @GetMapping
    public ContradictoireTimelineResponse timeline(@PathVariable UUID caseFileId,
                                                   @AuthenticationPrincipal OidcUser oidcUser,
                                                   Principal principal) {
        return service.timeline(caseFileId, oidcUser, principal);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContradictoireRoundResponse create(@PathVariable UUID caseFileId,
                                              @RequestBody @jakarta.validation.Valid ContradictoireRoundRequest request,
                                              @AuthenticationPrincipal OidcUser oidcUser,
                                              Principal principal) {
        return service.create(caseFileId, request, oidcUser, principal);
    }

    @PutMapping("/{roundId}")
    public ContradictoireRoundResponse update(@PathVariable UUID caseFileId,
                                              @PathVariable UUID roundId,
                                              @RequestBody @jakarta.validation.Valid ContradictoireRoundRequest request,
                                              @AuthenticationPrincipal OidcUser oidcUser,
                                              Principal principal) {
        return service.update(caseFileId, roundId, request, oidcUser, principal);
    }

    @DeleteMapping("/{roundId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID caseFileId,
                       @PathVariable UUID roundId,
                       @AuthenticationPrincipal OidcUser oidcUser,
                       Principal principal) {
        service.delete(caseFileId, roundId, oidcUser, principal);
    }
}
