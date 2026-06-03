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
 * SF-218-47 : service applicatif de l'outil "Congé de proche aidant"
 * (art. L.3142-16 à L.3142-27 CT, F-DT-79) — détermine l'éligibilité (la
 * personne aidée doit résider en France/EEE), la durée maximale (12 mois sur la
 * carrière) et une estimation indicative de l'AJPA. Outil <b>FRANCE
 * UNIQUEMENT</b>, distinct du congé parental d'éducation (F-DT-78) et des congés
 * pour évènements familiaux (F-DT-76).
 *
 * <p>Gates :
 * <ul>
 *   <li>workspace.country = FRANCE (sinon 400 — outil FR-only) ;</li>
 *   <li>caseFile.legalDomain = DROIT_DU_TRAVAIL (sinon 400) ;</li>
 *   <li>champs requis présents et valides (sinon 400) ;</li>
 *   <li>isolation workspace par {@code WorkspaceMemberRepository} (sinon 404).</li>
 * </ul>
 */
@Service
public class CongeProcheAidantService {

    private final CongeProcheAidantRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public CongeProcheAidantService(CongeProcheAidantRepository repository,
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
    public CongeProcheAidantResponse analyze(UUID caseFileId,
                                             CongeProcheAidantRequest request,
                                             OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Congé de proche aidant — outil FRANCE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        CongeProcheAidantResult result;
        try {
            result = CongeProcheAidantAnalyzer.analyze(
                    request.lienPersonneAidee(),
                    request.personneAideeResideFrance(),
                    request.dureeSouhaiteeMois(),
                    request.ajpaDemandee());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        CongeProcheAidantAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    CongeProcheAidantAnalysis a = new CongeProcheAidantAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setStatut(result.statut());
        entity.setLienPersonneAidee(result.lienPersonneAidee());
        entity.setPersonneAideeResideFrance(result.personneAideeResideFrance());
        entity.setDureeSouhaiteeMois(result.dureeSouhaiteeMois());
        entity.setDureeMaxMois(result.dureeMaxMois());
        entity.setDureeRetenueMois(result.dureeRetenueMois());
        entity.setAjpaDemandee(result.ajpaDemandee());
        entity.setEstimationAjpa(result.estimationAjpa());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public CongeProcheAidantResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        CongeProcheAidantAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de congé de proche aidant trouvée pour ce dossier"));
        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        return toResponse(caseFileId, country, deserialize(entity.getResultData()));
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

    private CongeProcheAidantResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, CongeProcheAidantResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private CongeProcheAidantResponse toResponse(UUID caseFileId, String country,
                                                 CongeProcheAidantResult r) {
        return new CongeProcheAidantResponse(
                caseFileId,
                r.statut(),
                r.lienPersonneAidee(),
                r.personneAideeResideFrance(),
                r.dureeSouhaiteeMois(),
                r.dureeMaxMois(),
                r.dureeRetenueMois(),
                r.ajpaDemandee(),
                r.ajpaJournaliere(),
                r.estimationAjpa(),
                r.protectionEmploi(),
                r.nonImputableCongesPayes(),
                r.notes(),
                country,
                r.baseJuridique());
    }
}
