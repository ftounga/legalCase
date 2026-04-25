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
import java.util.List;
import java.util.UUID;

/**
 * SF-DT-13-01 : service orchestrant le calcul du risque de requalification d'un licenciement
 * pour motif économique (FR — art. L.1233-3, L.1233-4, L.1233-5, L.1233-45).
 */
@Service
public class LicenciementEconomiqueService {

    private final LicenciementEconomiqueRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public LicenciementEconomiqueService(LicenciementEconomiqueRepository repository,
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
    public LicenciementEconomiqueResponse calculate(UUID caseFileId,
                                                    LicenciementEconomiqueRequest request,
                                                    OidcUser oidcUser, Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.motifEconomiqueInvoque() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Motif économique invoqué requis");
        }
        if (request.criteresOrdreAppliques() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Critères d'ordre appliqués requis");
        }
        if (request.tentativesReclassement() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tentatives de reclassement requises");
        }
        if (request.dateNotification() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date de notification requise");
        }

        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace().getCountry();

        LicenciementEconomiqueResult result;
        try {
            result = LicenciementEconomiqueCalculator.compute(
                    request.motifEconomiqueInvoque(),
                    nullSafe(request.preuvesMotif()),
                    request.criteresOrdreAppliques(),
                    request.salarieAge() != null ? request.salarieAge() : 0,
                    request.salarieAncienneteMois() != null ? request.salarieAncienneteMois() : 0,
                    request.salarieChargesFamille() != null ? request.salarieChargesFamille() : 0,
                    request.salarieQualitesProf(),
                    request.tentativesReclassement(),
                    Boolean.TRUE.equals(request.prioriteReembauchePropose()),
                    Boolean.TRUE.equals(request.congeReclassementPropose()),
                    request.dateNotification(),
                    country
            );
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        LicenciementEconomiqueAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    LicenciementEconomiqueAnalysis a = new LicenciementEconomiqueAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setMotifEconomiqueInvoque(result.motifEconomiqueInvoque());
        entity.setPreuvesMotifJson(serialize(result.preuvesMotif()));
        entity.setCriteresOrdreJson(serialize(result.criteresOrdreAppliques()));
        entity.setSalarieAge(result.salarieAge());
        entity.setSalarieAncienneteMois(result.salarieAncienneteMois());
        entity.setSalarieChargesFamille(result.salarieChargesFamille());
        entity.setSalarieQualitesProf(result.salarieQualitesProf());
        entity.setTentativesReclassementJson(serialize(result.tentativesReclassement()));
        entity.setPrioriteReembauchePropose(result.prioriteReembauchePropose());
        entity.setCongeReclassementPropose(result.congeReclassementPropose());
        entity.setDateNotification(result.dateNotification());
        entity.setCountry(result.country());
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, result);
    }

    @Transactional(readOnly = true)
    public LicenciementEconomiqueResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        LicenciementEconomiqueAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de licenciement économique trouvée pour ce dossier"));
        return toResponse(caseFileId, deserialize(entity.getResultData()));
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

    private LicenciementEconomiqueResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, LicenciementEconomiqueResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private static <T> List<T> nullSafe(List<T> list) {
        return list != null ? list : List.of();
    }

    private LicenciementEconomiqueResponse toResponse(UUID caseFileId, LicenciementEconomiqueResult r) {
        return new LicenciementEconomiqueResponse(
                caseFileId,
                r.motifEconomiqueInvoque(),
                r.preuvesMotif() != null ? r.preuvesMotif() : List.of(),
                r.criteresOrdreAppliques() != null ? r.criteresOrdreAppliques() : List.of(),
                r.salarieAge(),
                r.salarieAncienneteMois(),
                r.salarieChargesFamille(),
                r.salarieQualitesProf(),
                r.tentativesReclassement() != null ? r.tentativesReclassement() : List.of(),
                r.prioriteReembauchePropose(),
                r.congeReclassementPropose(),
                r.dateNotification(),
                r.scoreCausalite(),
                r.scoreCriteresOrdre(),
                r.scoreReclassement(),
                r.scoreGlobal(),
                r.verdictRisqueRequalification(),
                r.criteresOrdreManquants() != null ? r.criteresOrdreManquants() : List.of(),
                r.criteresOrdreObligatoiresOk(),
                r.obligationReclassementRespectee(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : List.of(),
                r.country()
        );
    }
}
