package fr.ailegalcase.casefile;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * F-283 / SF-283-01 — endpoints des phases procédurales (progression datée) d'un
 * dossier. Isolation workspace via le dossier (cf. {@link CasePhaseService}).
 */
@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/phases")
@Validated
public class CasePhaseController {

    private final CasePhaseService service;

    public CasePhaseController(CasePhaseService service) {
        this.service = service;
    }

    @GetMapping
    public CasePhaseTimelineResponse timeline(@PathVariable UUID caseFileId,
                                              @AuthenticationPrincipal OidcUser oidcUser,
                                              Principal principal) {
        return service.timeline(caseFileId, oidcUser, principal);
    }

    @GetMapping("/suggestions")
    public List<CasePhaseSuggestion> suggestions(@PathVariable UUID caseFileId,
                                                 @AuthenticationPrincipal OidcUser oidcUser,
                                                 Principal principal) {
        return service.suggestions(caseFileId, oidcUser, principal);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CasePhaseResponse create(@PathVariable UUID caseFileId,
                                    @RequestBody @jakarta.validation.Valid CasePhaseRequest request,
                                    @AuthenticationPrincipal OidcUser oidcUser,
                                    Principal principal) {
        return service.create(caseFileId, request, oidcUser, principal);
    }

    @PutMapping("/{phaseId}")
    public CasePhaseResponse update(@PathVariable UUID caseFileId,
                                    @PathVariable UUID phaseId,
                                    @RequestBody @jakarta.validation.Valid CasePhaseRequest request,
                                    @AuthenticationPrincipal OidcUser oidcUser,
                                    Principal principal) {
        return service.update(caseFileId, phaseId, request, oidcUser, principal);
    }

    @DeleteMapping("/{phaseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID caseFileId,
                       @PathVariable UUID phaseId,
                       @AuthenticationPrincipal OidcUser oidcUser,
                       Principal principal) {
        service.delete(caseFileId, phaseId, oidcUser, principal);
    }
}
