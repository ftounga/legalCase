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
 * SF-DT-14-01 : service orchestrant le calcul de validité d'un PSE
 * (FR — art. L.1233-24-1, L.1233-30, L.1233-57-2, L.1233-61, L.1235-7-1).
 */
@Service
public class PseService {

    private final PseRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public PseService(PseRepository repository,
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
    public PseResponse calculate(UUID caseFileId,
                                 PseRequest request,
                                 OidcUser oidcUser, Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.tailleEntrepriseSalaries() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Taille entreprise (salariés) requise");
        }
        if (request.nombreLicenciementsEnvisages() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nombre de licenciements envisagés requis");
        }
        if (request.periodeJours() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Période (jours) requise");
        }
        if (request.modeAdoption() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Mode d'adoption requis");
        }
        if (request.csaeConsulteAvis() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Avis du CSE requis");
        }
        if (request.dreetsStatut() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statut DREETS requis");
        }
        if (request.dateProjet() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Date du projet requise");
        }

        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace().getCountry();

        PseResult result;
        try {
            result = PseCalculator.compute(
                    request.tailleEntrepriseSalaries(),
                    request.nombreLicenciementsEnvisages(),
                    request.periodeJours(),
                    request.modeAdoption(),
                    request.csaeConsulteAvis(),
                    request.dreetsStatut(),
                    request.dateNotificationDreets(),
                    nullSafe(request.contenuMesures()),
                    request.dateProjet(),
                    country
            );
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        PseAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    PseAnalysis a = new PseAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setTailleEntrepriseSalaries(result.tailleEntrepriseSalaries());
        entity.setNombreLicenciementsEnvisages(result.nombreLicenciementsEnvisages());
        entity.setPeriodeJours(result.periodeJours());
        entity.setModeAdoption(result.modeAdoption());
        entity.setCsaeConsulteAvis(result.csaeConsulteAvis());
        entity.setDreetsStatut(result.dreetsStatut());
        entity.setDateNotificationDreets(result.dateNotificationDreets());
        entity.setContenuMesuresJson(serialize(result.contenuMesures()));
        entity.setDateProjet(result.dateProjet());
        entity.setCountry(result.country());
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, result);
    }

    @Transactional(readOnly = true)
    public PseResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        PseAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse PSE trouvée pour ce dossier"));
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

    private PseResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, PseResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private static <T> List<T> nullSafe(List<T> list) {
        return list != null ? list : List.of();
    }

    private PseResponse toResponse(UUID caseFileId, PseResult r) {
        return new PseResponse(
                caseFileId,
                r.pseRequis(),
                r.scoreConformite(),
                r.verdictValidite(),
                r.criteresRemplis() != null ? r.criteresRemplis() : List.of(),
                r.criteresManquants() != null ? r.criteresManquants() : List.of(),
                r.delaiContestationJours(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : List.of(),
                r.country()
        );
    }
}
