package fr.ailegalcase.casefile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-212-31 : service orchestrant la vérification de conformité du
 * processus électoral CSE (F-DT-65-elections-cse-conformite, FRANCE —
 * L. 2314-1 à L. 2314-37 CT ; R. 2314-1+ CT ; ordonnances Macron 22/09/2017).
 * Persistance snapshot — un seul résultat courant par dossier
 * (UNIQUE(case_file_id) côté migration 345).
 */
@Service
public class ElectionsCseConformiteService {

    private final ElectionsCseConformiteRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public ElectionsCseConformiteService(ElectionsCseConformiteRepository repository,
                                         CaseFileRepository caseFileRepository,
                                         WorkspaceMemberRepository workspaceMemberRepository,
                                         CurrentUserResolver currentUserResolver,
                                         ObjectMapper objectMapper) {
        this.repository = repository;
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ElectionsCseConformiteResponse calculate(UUID caseFileId,
                                                    ElectionsCseConformiteRequest request,
                                                    OidcUser oidcUser, Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Body requis");
        }

        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace().getCountry();

        ElectionsCseConformiteCalculator.Result result;
        try {
            result = ElectionsCseConformiteCalculator.compute(
                    request.toInput(), country, LocalDate.now());
        } catch (IllegalArgumentException e) {
            if (e.getMessage() != null && e.getMessage().contains("FRANCE")) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, e.getMessage());
            }
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        ElectionsCseConformiteResponse response =
                toResponse(caseFileId, request, result, Instant.now());

        ElectionsCseConformiteAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    ElectionsCseConformiteAnalysis a = new ElectionsCseConformiteAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setCountry(result.country());
        entity.setSnapshotData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public ElectionsCseConformiteResponse get(UUID caseFileId,
                                              OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        ElectionsCseConformiteAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de conformité élections CSE trouvée pour ce dossier"));
        return deserialize(entity.getSnapshotData());
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        return currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }

    private CaseFile resolveCaseFile(UUID caseFileId, User user) {
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dossier introuvable"));
        boolean member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(cf.getWorkspace().getId())).orElse(false);
        if (!member) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Ce dossier appartient à un autre workspace");
        }
        if (!"DROIT_DU_TRAVAIL".equals(cf.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Ce dossier n'est pas un dossier de droit du travail");
        }
        return cf;
    }

    private String serialize(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation");
        }
    }

    private ElectionsCseConformiteResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, ElectionsCseConformiteResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private ElectionsCseConformiteResponse toResponse(UUID caseFileId,
                                                      ElectionsCseConformiteRequest req,
                                                      ElectionsCseConformiteCalculator.Result r,
                                                      Instant calculatedAt) {
        return new ElectionsCseConformiteResponse(
                caseFileId,
                req.effectifEntreprise(),
                req.papNegocieAvecOS(),
                req.delaiInvitationOSRespectee(),
                req.collegesConformes(),
                req.resultatsContestes(),
                req.dateElection(),
                req.motifContestation(),
                r.analyseElectionsCse(),
                r.scoreConformite(),
                r.pointsIrregularite(),
                r.delaiContestationJours(),
                r.dateLimitContestationSiConnue(),
                r.alerteDelaiContestation(),
                r.basesJuridiques(),
                r.messages(),
                r.country(),
                calculatedAt
        );
    }
}
