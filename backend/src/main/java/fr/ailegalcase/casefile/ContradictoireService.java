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

/**
 * F-282 / SF-282-01 — gestion du cycle contradictoire (rounds) d'un dossier.
 * Isolation workspace via le dossier (mirror {@link CaseNoteService}). Calcule le
 * résumé « round courant / à qui le tour / prochaine échéance » pour le fil rouge.
 */
@Service
public class ContradictoireService {

    private final ContradictoireRoundRepository roundRepository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;

    public ContradictoireService(ContradictoireRoundRepository roundRepository,
                                 CaseFileRepository caseFileRepository,
                                 WorkspaceMemberRepository workspaceMemberRepository,
                                 CurrentUserResolver currentUserResolver) {
        this.roundRepository = roundRepository;
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
    }

    @Transactional(readOnly = true)
    public ContradictoireTimelineResponse timeline(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFileForUser(caseFileId, user);
        List<ContradictoireRound> rounds = roundRepository.findByCaseFileIdOrderByRoundNumberAsc(caseFile.getId());
        return new ContradictoireTimelineResponse(
                rounds.stream().map(ContradictoireRoundResponse::from).toList(),
                computeSummary(rounds));
    }

    @Transactional
    public ContradictoireRoundResponse create(UUID caseFileId, ContradictoireRoundRequest request,
                                              OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFileForUser(caseFileId, user);
        validate(request);

        int nextNumber = roundRepository.findFirstByCaseFileIdOrderByRoundNumberDesc(caseFile.getId())
                .map(r -> r.getRoundNumber() + 1).orElse(1);

        ContradictoireRound round = new ContradictoireRound();
        round.setCaseFile(caseFile);
        round.setRoundNumber(nextNumber);
        apply(round, request);
        return ContradictoireRoundResponse.from(roundRepository.save(round));
    }

    @Transactional
    public ContradictoireRoundResponse update(UUID caseFileId, UUID roundId, ContradictoireRoundRequest request,
                                              OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFileForUser(caseFileId, user);
        validate(request);
        ContradictoireRound round = resolveRoundForCaseFile(roundId, caseFile);
        apply(round, request);
        return ContradictoireRoundResponse.from(roundRepository.save(round));
    }

    @Transactional
    public void delete(UUID caseFileId, UUID roundId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFileForUser(caseFileId, user);
        ContradictoireRound round = resolveRoundForCaseFile(roundId, caseFile);
        roundRepository.delete(round);
    }

    // ── Logique ──────────────────────────────────────────────────────────────

    /**
     * Résumé pour le fil rouge. Aucun round → c'est à NOUS de saisir (round 1).
     * Sinon, le tour revient à la partie opposée au dernier déposant ; l'échéance
     * est celle du dernier round.
     */
    static ContradictoireTimelineResponse.Summary computeSummary(List<ContradictoireRound> rounds) {
        if (rounds.isEmpty()) {
            return new ContradictoireTimelineResponse.Summary(0, ContradictoireParty.OURS, null);
        }
        ContradictoireRound last = rounds.get(rounds.size() - 1);
        ContradictoireParty awaiting = last.getParty() == ContradictoireParty.ADVERSE
                ? ContradictoireParty.OURS : ContradictoireParty.ADVERSE;
        return new ContradictoireTimelineResponse.Summary(
                last.getRoundNumber(), awaiting, last.getResponseDueAt());
    }

    private void validate(ContradictoireRoundRequest request) {
        if (request.responseDueAt() != null && request.responseDueAt().isBefore(request.datedAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "responseDueAt must not be before datedAt");
        }
    }

    private void apply(ContradictoireRound round, ContradictoireRoundRequest request) {
        round.setParty(request.party());
        round.setLabel(request.label());
        round.setDatedAt(request.datedAt());
        round.setResponseDueAt(request.responseDueAt());
        round.setSourceDocumentId(request.sourceDocumentId());
        round.setSourceConclusionId(request.sourceConclusionId());
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

    private ContradictoireRound resolveRoundForCaseFile(UUID roundId, CaseFile caseFile) {
        ContradictoireRound round = roundRepository.findById(roundId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Round not found"));
        if (!round.getCaseFile().getId().equals(caseFile.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Round not found");
        }
        return round;
    }
}
