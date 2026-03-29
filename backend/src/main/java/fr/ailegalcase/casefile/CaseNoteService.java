package fr.ailegalcase.casefile;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.shared.OAuthProviderResolver;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Service
public class CaseNoteService {

    private final CaseNoteRepository noteRepository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;

    public CaseNoteService(CaseNoteRepository noteRepository,
                           CaseFileRepository caseFileRepository,
                           WorkspaceMemberRepository workspaceMemberRepository,
                           CurrentUserResolver currentUserResolver) {
        this.noteRepository = noteRepository;
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
    }

    @Transactional(readOnly = true)
    public List<CaseNoteResponse> list(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFileForUser(caseFileId, user);
        return noteRepository.findByCaseFileIdOrderByCreatedAtDesc(caseFile.getId())
                .stream().map(CaseNoteResponse::from).toList();
    }

    @Transactional
    public CaseNoteResponse create(UUID caseFileId, CaseNoteRequest request,
                                   OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFileForUser(caseFileId, user);

        CaseNote note = new CaseNote();
        note.setCaseFile(caseFile);
        note.setCreatedBy(user);
        note.setContent(request.content());
        return CaseNoteResponse.from(noteRepository.save(note));
    }

    @Transactional
    public CaseNoteResponse update(UUID caseFileId, UUID noteId, CaseNoteRequest request,
                                   OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFileForUser(caseFileId, user);
        CaseNote note = resolveNoteForAuthor(noteId, user);
        note.setContent(request.content());
        return CaseNoteResponse.from(noteRepository.save(note));
    }

    @Transactional
    public void delete(UUID caseFileId, UUID noteId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFileForUser(caseFileId, user);
        CaseNote note = resolveNoteForAuthor(noteId, user);
        noteRepository.delete(note);
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        String provider = OAuthProviderResolver.resolve(principal);
        return currentUserResolver.resolve(oidcUser, provider, principal);
    }

    private CaseFile resolveCaseFileForUser(UUID caseFileId, User user) {
        CaseFile caseFile = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        boolean member = workspaceMemberRepository
                .findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(caseFile.getWorkspace().getId()))
                .orElse(false);
        if (!member) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        return caseFile;
    }

    private CaseNote resolveNoteForAuthor(UUID noteId, User user) {
        CaseNote note = noteRepository.findById(noteId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Note not found"));
        if (!note.getCreatedBy().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not the note author");
        }
        return note;
    }
}
