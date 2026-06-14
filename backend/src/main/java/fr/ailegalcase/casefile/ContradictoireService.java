package fr.ailegalcase.casefile;

import fr.ailegalcase.auth.User;
import fr.ailegalcase.casefile.conclusion.CaseConclusionRepository;
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
    private final CaseConclusionRepository caseConclusionRepository;
    private final DocumentRepository documentRepository;

    public ContradictoireService(ContradictoireRoundRepository roundRepository,
                                 CaseFileRepository caseFileRepository,
                                 WorkspaceMemberRepository workspaceMemberRepository,
                                 CurrentUserResolver currentUserResolver,
                                 CaseConclusionRepository caseConclusionRepository,
                                 DocumentRepository documentRepository) {
        this.roundRepository = roundRepository;
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
        this.caseConclusionRepository = caseConclusionRepository;
        this.documentRepository = documentRepository;
    }

    @Transactional(readOnly = true)
    public ContradictoireTimelineResponse timeline(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFileForUser(caseFileId, user);
        List<ContradictoireRound> rounds = roundRepository.findByCaseFileIdOrderByRoundNumberAsc(caseFile.getId());
        return new ContradictoireTimelineResponse(
                rounds.stream()
                        .map(r -> ContradictoireRoundResponse.from(r, resolveSourceLabel(r, caseFile.getId())))
                        .toList(),
                computeSummary(rounds));
    }

    @Transactional
    public ContradictoireRoundResponse create(UUID caseFileId, ContradictoireRoundRequest request,
                                              OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFileForUser(caseFileId, user);
        validate(request, caseFile);

        int nextNumber = roundRepository.findFirstByCaseFileIdOrderByRoundNumberDesc(caseFile.getId())
                .map(r -> r.getRoundNumber() + 1).orElse(1);

        ContradictoireRound round = new ContradictoireRound();
        round.setCaseFile(caseFile);
        round.setRoundNumber(nextNumber);
        apply(round, request);
        ContradictoireRound saved = roundRepository.save(round);
        return ContradictoireRoundResponse.from(saved, resolveSourceLabel(saved, caseFile.getId()));
    }

    @Transactional
    public ContradictoireRoundResponse update(UUID caseFileId, UUID roundId, ContradictoireRoundRequest request,
                                              OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFileForUser(caseFileId, user);
        validate(request, caseFile);
        ContradictoireRound round = resolveRoundForCaseFile(roundId, caseFile);
        apply(round, request);
        ContradictoireRound saved = roundRepository.save(round);
        return ContradictoireRoundResponse.from(saved, resolveSourceLabel(saved, caseFile.getId()));
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

    /**
     * Valide la cohérence d'un round et l'isolation de ses sources (SF-282-03).
     * En plus de {@code responseDueAt >= datedAt} : tout id de source lié doit
     * appartenir AU dossier (faille cross-dossier/workspace), et le type de source
     * doit être cohérent avec la partie — un round « nous » (OURS) ne référence que
     * nos conclusions, un round adverse (ADVERSE) que le document du jeu adverse.
     */
    private void validate(ContradictoireRoundRequest request, CaseFile caseFile) {
        if (request.responseDueAt() != null && request.responseDueAt().isBefore(request.datedAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "responseDueAt must not be before datedAt");
        }

        // Cohérence partie ↔ source : un round ne porte qu'une source, du bon type.
        if (request.party() == ContradictoireParty.OURS && request.sourceDocumentId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "OURS round cannot reference an adverse document");
        }
        if (request.party() == ContradictoireParty.ADVERSE && request.sourceConclusionId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "ADVERSE round cannot reference our conclusions");
        }

        // Isolation : les ids liés doivent appartenir au dossier courant.
        if (request.sourceConclusionId() != null
                && caseConclusionRepository.findByIdAndCaseFileId(request.sourceConclusionId(), caseFile.getId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "sourceConclusionId does not belong to this case file");
        }
        if (request.sourceDocumentId() != null
                && documentRepository.findByIdAndCaseFile_Id(request.sourceDocumentId(), caseFile.getId()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "sourceDocumentId does not belong to this case file");
        }
    }

    /**
     * Libellé lisible de la source d'un round (lecture seule). {@code « Conclusions v{n} »}
     * pour une version liée, le nom de fichier pour un document lié, {@code null} si pas
     * de source <strong>ou</strong> si l'entité a été supprimée depuis (non-régressif).
     */
    private String resolveSourceLabel(ContradictoireRound round, UUID caseFileId) {
        if (round.getSourceConclusionId() != null) {
            return caseConclusionRepository.findByIdAndCaseFileId(round.getSourceConclusionId(), caseFileId)
                    .map(c -> "Conclusions v" + c.getVersionNumber())
                    .orElse(null);
        }
        if (round.getSourceDocumentId() != null) {
            return documentRepository.findByIdAndCaseFile_Id(round.getSourceDocumentId(), caseFileId)
                    .map(d -> d.getOriginalFilename())
                    .orElse(null);
        }
        return null;
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
