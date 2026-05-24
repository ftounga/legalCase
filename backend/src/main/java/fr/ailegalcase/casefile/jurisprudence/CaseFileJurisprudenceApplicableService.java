package fr.ailegalcase.casefile.jurisprudence;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.jurisprudencemapping.ConclusionsJurisprudenceContext;
import fr.ailegalcase.jurisprudencemapping.ToolJurisprudenceCitationByTool;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

/**
 * F-JU-02 / SF-JU-02-02 — service d'exposition API de
 * {@link ConclusionsJurisprudenceContext} pour le frontend (PDF synthèse).
 *
 * <p>Vérifie l'isolation workspace puis délègue à
 * {@link ConclusionsJurisprudenceContext#collectForCaseFile(UUID)} (service
 * déjà livré SF-JU-02-01). Pattern miroir
 * {@link JurisprudenceCitationService}.</p>
 *
 * <p>Le service ne porte aucune logique métier d'agrégation : le mapping
 * outils → arrêts + déduplication par {@code arret_ref} reste exclusivement
 * dans {@code ConclusionsJurisprudenceContext}.</p>
 */
@Service
public class CaseFileJurisprudenceApplicableService {

    private final CaseFileRepository caseFileRepository;
    private final CurrentUserResolver currentUserResolver;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final ConclusionsJurisprudenceContext jurisprudenceContext;

    public CaseFileJurisprudenceApplicableService(
            CaseFileRepository caseFileRepository,
            CurrentUserResolver currentUserResolver,
            WorkspaceMemberRepository workspaceMemberRepository,
            ConclusionsJurisprudenceContext jurisprudenceContext) {
        this.caseFileRepository = caseFileRepository;
        this.currentUserResolver = currentUserResolver;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.jurisprudenceContext = jurisprudenceContext;
    }

    /**
     * Retourne la jurisprudence applicable pour un dossier.
     *
     * @throws ResponseStatusException {@code 404} si le dossier est inconnu ou
     *                                 appartient à un autre workspace
     *                                 (cohérence pattern existant —
     *                                 pas de fuite d'existence)
     */
    @Transactional(readOnly = true)
    public JurisprudenceApplicableResponse getForCaseFile(UUID caseFileId, OidcUser oidcUser,
                                                          String provider, Principal principal) {
        Workspace workspace = resolveWorkspace(oidcUser, provider, principal);
        resolveCaseFileInWorkspace(caseFileId, workspace);

        List<ToolJurisprudenceCitationByTool> raw = jurisprudenceContext.collectForCaseFile(caseFileId);
        List<JurisprudenceApplicableEntry> entries = raw.stream()
                .map(entry -> new JurisprudenceApplicableEntry(
                        entry.toolId(),
                        entry.brancheCalculId(),
                        entry.citations().stream()
                                .map(JurisprudenceCitationDto::from)
                                .toList()))
                .toList();
        return new JurisprudenceApplicableResponse(entries);
    }

    private Workspace resolveWorkspace(OidcUser oidcUser, String provider, Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, provider, principal);
        return workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"))
                .getWorkspace();
    }

    /** Charge le dossier et vérifie l'isolation workspace — 404 (pas de fuite d'existence). */
    private CaseFile resolveCaseFileInWorkspace(UUID caseFileId, Workspace workspace) {
        CaseFile caseFile = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        if (!caseFile.getWorkspace().getId().equals(workspace.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }
        return caseFile;
    }
}
