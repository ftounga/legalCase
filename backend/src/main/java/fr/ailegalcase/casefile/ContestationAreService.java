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
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * SF-DT-35-01 : service orchestrant l'analyse de contestation d'une décision
 * France Travail (ex-Pôle emploi) — gate FRANCE + DROIT_DU_TRAVAIL strict.
 */
@Service
public class ContestationAreService {

    private final ContestationAreRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public ContestationAreService(ContestationAreRepository repository,
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
    public ContestationAreResponse calculate(UUID caseFileId,
                                             ContestationAreRequest request,
                                             OidcUser oidcUser,
                                             Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Contestation ARE FR uniquement (art. L.5422-1) — équivalent BE = procédure ONEM");
        }

        if (request.demandeurDejaSaisiTribunal() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "demandeurDejaSaisiTribunal est requis");
        }
        if (request.delaiContestationRespecte() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "delaiContestationRespecte est requis");
        }

        ContestationAreResult result;
        try {
            result = ContestationAreCalculator.compute(
                    request.typeDecisionContestee(),
                    request.motifContestation(),
                    request.dateNotificationDecision(),
                    request.dateRecoursHierarchiquePropose(),
                    request.preuvesProduites() != null ? request.preuvesProduites() : Collections.emptyList(),
                    request.montantContesteEur(),
                    request.demandeurDejaSaisiTribunal(),
                    request.delaiContestationRespecte());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        ContestationAreResponse response = toResponse(caseFileId, country, result);

        ContestationAreAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    ContestationAreAnalysis a = new ContestationAreAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setCountry(country);
        entity.setSnapshotData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public ContestationAreResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        ContestationAreAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de contestation ARE trouvée pour ce dossier"));
        return deserialize(entity.getSnapshotData());
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        return currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }

    private CaseFile resolveCaseFile(UUID caseFileId, User user) {
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        boolean member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(cf.getWorkspace().getId())).orElse(false);
        if (!member) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }
        if (!"DROIT_DU_TRAVAIL".equals(cf.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
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

    private ContestationAreResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, ContestationAreResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private ContestationAreResponse toResponse(UUID caseFileId, String country, ContestationAreResult r) {
        return new ContestationAreResponse(
                caseFileId,
                r.typeDecisionContestee(),
                r.motifContestation(),
                r.dateNotificationDecision(),
                r.dateRecoursHierarchiquePropose(),
                r.preuvesProduites() != null ? r.preuvesProduites() : List.of(),
                r.montantContesteEur(),
                r.demandeurDejaSaisiTribunal(),
                r.delaiContestationRespecte(),
                r.delaiRecoursHierarchiqueJoursOk(),
                r.delaiRecoursContentieuxTaJoursOk(),
                r.scoreSuccessProbable(),
                r.verdictRecommandation(),
                r.delaiInstructionMoisPrevisionnel(),
                r.expertiseTraitementSalaireRecommandee(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : List.of(),
                country);
    }
}
