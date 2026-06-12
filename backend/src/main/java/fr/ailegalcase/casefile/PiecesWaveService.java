package fr.ailegalcase.casefile;

import fr.ailegalcase.analysis.AnalysisJob;
import fr.ailegalcase.analysis.AnalysisJobRepository;
import fr.ailegalcase.analysis.AnalysisStatus;
import fr.ailegalcase.analysis.JobType;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.document.Document;
import fr.ailegalcase.document.DocumentRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.shared.OAuthProviderResolver;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * F-283 / SF-283-02 — calcule la « vague de pièces » d'un dossier : les pièces
 * ajoutées depuis la dernière analyse dossier réussie. Lecture seule, dérivée
 * de {@code analysis_jobs} (job {@link JobType#CASE_ANALYSIS} {@link AnalysisStatus#DONE})
 * et de {@code documents}. N'introduit aucune pipeline ni appel IA : rend lisible
 * un état déjà calculable, le CTA route vers la ré-analyse existante.
 * Isolation workspace via le dossier (mirror {@link ContradictoireService}).
 */
@Service
public class PiecesWaveService {

    private final AnalysisJobRepository analysisJobRepository;
    private final DocumentRepository documentRepository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;

    public PiecesWaveService(AnalysisJobRepository analysisJobRepository,
                             DocumentRepository documentRepository,
                             CaseFileRepository caseFileRepository,
                             WorkspaceMemberRepository workspaceMemberRepository,
                             CurrentUserResolver currentUserResolver) {
        this.analysisJobRepository = analysisJobRepository;
        this.documentRepository = documentRepository;
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
    }

    @Transactional(readOnly = true)
    public PiecesWaveResponse wave(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFileForUser(caseFileId, user);

        Instant analyzedAt = lastSuccessfulAnalysisAt(caseFile.getId());
        // Dossier neuf / jamais analysé : ce n'est pas une vague (l'onboarding
        // analyse initial est couvert ailleurs). Pas de carte.
        if (analyzedAt == null) {
            return PiecesWaveResponse.none();
        }

        List<Document> pending = documentRepository
                .findByCaseFile_IdAndCreatedAtAfterOrderByCreatedAtDesc(caseFile.getId(), analyzedAt);
        List<PiecesWaveResponse.PendingPiece> pieces = pending.stream()
                .map(d -> new PiecesWaveResponse.PendingPiece(
                        d.getId(), d.getOriginalFilename(), d.getCreatedAt()))
                .toList();
        return new PiecesWaveResponse(analyzedAt, pieces.size(), pieces);
    }

    /**
     * Horodatage de la dernière analyse dossier réussie. Le job CASE_ANALYSIS est
     * unique par dossier (upsert) : on le lit et on ne retient son {@code updatedAt}
     * que s'il est DONE. {@code null} si aucune analyse réussie.
     */
    private Instant lastSuccessfulAnalysisAt(UUID caseFileId) {
        return analysisJobRepository.findByCaseFileIdAndJobType(caseFileId, JobType.CASE_ANALYSIS)
                .filter(j -> j.getStatus() == AnalysisStatus.DONE)
                .map(AnalysisJob::getUpdatedAt)
                .orElse(null);
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
}
