package fr.ailegalcase.casefile;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/case-files/{caseFileId}/notes")
@Validated
public class CaseNoteController {

    private final CaseNoteService noteService;

    public CaseNoteController(CaseNoteService noteService) {
        this.noteService = noteService;
    }

    @GetMapping
    public List<CaseNoteResponse> list(@PathVariable UUID caseFileId,
                                       @AuthenticationPrincipal OidcUser oidcUser,
                                       Principal principal) {
        return noteService.list(caseFileId, oidcUser, principal);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CaseNoteResponse create(@PathVariable UUID caseFileId,
                                   @RequestBody @jakarta.validation.Valid CaseNoteRequest request,
                                   @AuthenticationPrincipal OidcUser oidcUser,
                                   Principal principal) {
        return noteService.create(caseFileId, request, oidcUser, principal);
    }

    @PutMapping("/{noteId}")
    public CaseNoteResponse update(@PathVariable UUID caseFileId,
                                   @PathVariable UUID noteId,
                                   @RequestBody @jakarta.validation.Valid CaseNoteRequest request,
                                   @AuthenticationPrincipal OidcUser oidcUser,
                                   Principal principal) {
        return noteService.update(caseFileId, noteId, request, oidcUser, principal);
    }

    @DeleteMapping("/{noteId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID caseFileId,
                       @PathVariable UUID noteId,
                       @AuthenticationPrincipal OidcUser oidcUser,
                       Principal principal) {
        noteService.delete(caseFileId, noteId, oidcUser, principal);
    }
}
