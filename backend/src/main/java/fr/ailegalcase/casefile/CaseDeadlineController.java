package fr.ailegalcase.casefile;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/deadlines")
@Validated
public class CaseDeadlineController {

    private final CaseDeadlineService deadlineService;

    public CaseDeadlineController(CaseDeadlineService deadlineService) {
        this.deadlineService = deadlineService;
    }

    @GetMapping
    public List<CaseDeadlineResponse> list(@PathVariable UUID caseFileId,
                                           @AuthenticationPrincipal OidcUser oidcUser,
                                           Principal principal) {
        return deadlineService.list(caseFileId, oidcUser, principal);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CaseDeadlineResponse create(@PathVariable UUID caseFileId,
                                       @RequestBody @jakarta.validation.Valid CaseDeadlineRequest request,
                                       @AuthenticationPrincipal OidcUser oidcUser,
                                       Principal principal) {
        return deadlineService.create(caseFileId, request, oidcUser, principal);
    }

    @PutMapping("/{deadlineId}")
    public CaseDeadlineResponse update(@PathVariable UUID caseFileId,
                                       @PathVariable UUID deadlineId,
                                       @RequestBody @jakarta.validation.Valid CaseDeadlineRequest request,
                                       @AuthenticationPrincipal OidcUser oidcUser,
                                       Principal principal) {
        return deadlineService.update(caseFileId, deadlineId, request, oidcUser, principal);
    }

    @DeleteMapping("/{deadlineId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID caseFileId,
                       @PathVariable UUID deadlineId,
                       @AuthenticationPrincipal OidcUser oidcUser,
                       Principal principal) {
        deadlineService.delete(caseFileId, deadlineId, oidcUser, principal);
    }

    @PatchMapping("/{deadlineId}/validate")
    public ResponseEntity<CaseDeadlineResponse> validate(
            @PathVariable UUID caseFileId,
            @PathVariable UUID deadlineId,
            @RequestBody @jakarta.validation.Valid CaseDeadlineValidateRequest request,
            @AuthenticationPrincipal OidcUser oidcUser,
            Principal principal) {
        CaseDeadlineResponse resp = deadlineService.validate(caseFileId, deadlineId, request.action(), oidcUser, principal);
        return resp != null ? ResponseEntity.ok(resp) : ResponseEntity.noContent().build();
    }
}
