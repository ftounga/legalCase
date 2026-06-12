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
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * F-283 / SF-283-01 — gestion des phases procédurales (progression datée) d'un
 * dossier. Isolation workspace via le dossier (mirror {@link ContradictoireService}).
 * Calcule la phase courante (la transition la plus récente) pour la mise en exergue.
 */
@Service
public class CasePhaseService {

    private final CasePhaseRepository phaseRepository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;

    public CasePhaseService(CasePhaseRepository phaseRepository,
                            CaseFileRepository caseFileRepository,
                            WorkspaceMemberRepository workspaceMemberRepository,
                            CurrentUserResolver currentUserResolver) {
        this.phaseRepository = phaseRepository;
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
    }

    @Transactional(readOnly = true)
    public CasePhaseTimelineResponse timeline(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFileForUser(caseFileId, user);
        List<CasePhase> phases = sorted(phaseRepository.findByCaseFileIdOrderByEnteredAtAsc(caseFile.getId()));
        return new CasePhaseTimelineResponse(
                phases.stream().map(CasePhaseResponse::from).toList(),
                currentPhase(phases));
    }

    @Transactional
    public CasePhaseResponse create(UUID caseFileId, CasePhaseRequest request,
                                    OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFileForUser(caseFileId, user);

        CasePhase phase = new CasePhase();
        phase.setCaseFile(caseFile);
        apply(phase, request);
        return CasePhaseResponse.from(phaseRepository.save(phase));
    }

    @Transactional
    public CasePhaseResponse update(UUID caseFileId, UUID phaseId, CasePhaseRequest request,
                                    OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFileForUser(caseFileId, user);
        CasePhase phase = resolvePhaseForCaseFile(phaseId, caseFile);
        apply(phase, request);
        return CasePhaseResponse.from(phaseRepository.save(phase));
    }

    @Transactional
    public void delete(UUID caseFileId, UUID phaseId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFileForUser(caseFileId, user);
        CasePhase phase = resolvePhaseForCaseFile(phaseId, caseFile);
        phaseRepository.delete(phase);
    }

    // ── Logique ──────────────────────────────────────────────────────────────

    /**
     * Tri déterministe : date d'entrée croissante, puis ordre du référentiel
     * (l'enum est persisté en STRING, donc le tri par ordre métier se fait ici).
     */
    static List<CasePhase> sorted(List<CasePhase> phases) {
        return phases.stream()
                .sorted(Comparator.comparing(CasePhase::getEnteredAt)
                        .thenComparingInt(p -> p.getPhase().getOrder()))
                .toList();
    }

    /** Phase courante = la dernière transition de la frise ordonnée (null si aucune). */
    static CasePhaseType currentPhase(List<CasePhase> sortedPhases) {
        if (sortedPhases.isEmpty()) {
            return null;
        }
        return sortedPhases.get(sortedPhases.size() - 1).getPhase();
    }

    private void apply(CasePhase phase, CasePhaseRequest request) {
        phase.setPhase(request.phase());
        phase.setLabel(request.label());
        phase.setEnteredAt(request.enteredAt());
        phase.setNote(request.note());
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

    private CasePhase resolvePhaseForCaseFile(UUID phaseId, CaseFile caseFile) {
        CasePhase phase = phaseRepository.findById(phaseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Phase not found"));
        if (!phase.getCaseFile().getId().equals(caseFile.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Phase not found");
        }
        return phase;
    }
}
