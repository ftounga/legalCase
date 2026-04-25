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
import java.util.UUID;

/**
 * SF-DT-18-01 : service applicatif de l'outil d'indemnité de fin de mission
 * intérim (art. L.1251-32 Code du travail). Pattern miroir strict de
 * IndemnitePrecariteCddService (F-DT-17), situation métier distincte.
 */
@Service
public class IndemniteFinMissionInterimService {

    private final IndemniteFinMissionInterimRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public IndemniteFinMissionInterimService(IndemniteFinMissionInterimRepository repository,
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
    public IndemniteFinMissionInterimResponse calculate(UUID caseFileId,
                                                        IndemniteFinMissionInterimRequest request,
                                                        OidcUser oidcUser,
                                                        Principal principal) {
        if (request == null || request.dureeMissionJours() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Durée de la mission requise et strictement positive (en jours)");
        }
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        IndemniteFinMissionInterimResult result;
        try {
            result = IndemniteFinMissionInterimCalculator.compute(
                    request.totalRemunerationsBrutesEur(),
                    request.dureeMissionJours(),
                    request.motifExclusion(),
                    request.dateFinMission());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        IndemniteFinMissionInterimAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    IndemniteFinMissionInterimAnalysis a = new IndemniteFinMissionInterimAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setTotalRemunerationsBrutesEur(result.totalRemunerationsBrutesEur());
        entity.setDureeMissionJours(result.dureeMissionJours());
        entity.setMotifExclusion(result.motifExclusion());
        entity.setDateFinMission(result.dateFinMission());
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, result);
    }

    @Transactional(readOnly = true)
    public IndemniteFinMissionInterimResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        IndemniteFinMissionInterimAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse d'indemnité de fin de mission intérim trouvée pour ce dossier"));
        IndemniteFinMissionInterimResult result = deserialize(entity.getResultData());
        return toResponse(caseFileId, result);
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        return currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }

    private CaseFile resolveCaseFile(UUID caseFileId, User user) {
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        boolean member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(cf.getWorkspace().getId())).orElse(false);
        if (!member) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        if (!"DROIT_DU_TRAVAIL".equals(cf.getLegalDomain()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit du travail");
        return cf;
    }

    private String serialize(Object o) {
        try { return objectMapper.writeValueAsString(o); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation");
        }
    }

    private IndemniteFinMissionInterimResult deserialize(String json) {
        try { return objectMapper.readValue(json, IndemniteFinMissionInterimResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private IndemniteFinMissionInterimResponse toResponse(UUID caseFileId, IndemniteFinMissionInterimResult r) {
        return new IndemniteFinMissionInterimResponse(
                caseFileId,
                r.totalRemunerationsBrutesEur(),
                r.dureeMissionJours(),
                r.motifExclusion(),
                r.dateFinMission(),
                r.tauxApplique(),
                r.montantIndemniteEur(),
                r.exclusionRetenue(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : java.util.List.of(),
                r.country()
        );
    }
}
