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
 * SF-FA-19-03 : service applicatif changement de résidence enfant —
 * déménagement parent (art. 373-2 al. 3 Cciv).
 * Gate FR + DROIT_FAMILLE, isolation workspace, persistance JSON.
 */
@Service
public class ChangementResidenceService {

    private final ChangementResidenceRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public ChangementResidenceService(ChangementResidenceRepository repository,
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
    public ChangementResidenceResponse calculate(UUID caseFileId,
                                                 ChangementResidenceRequest request,
                                                 OidcUser oidcUser,
                                                 Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Changement de résidence : procédure propre au droit "
                            + "français (art. 373-2 al. 3 Cciv). F-FA-19 BE non couverte.");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Corps de requête requis");
        }
        if (request.dateChangementPrevu() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "dateChangementPrevu est requise");
        }
        if (request.distanceKm() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "distanceKm est requise");
        }
        if (request.raisonChangement() == null || request.raisonChangement().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "raisonChangement est requise");
        }
        if (request.modeResidenceActuel() == null
                || request.modeResidenceActuel().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "modeResidenceActuel est requis");
        }
        if (request.delaiInformationJours() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "delaiInformationJours est requis");
        }

        boolean consentement = Boolean.TRUE.equals(request.consentementAutreParent());
        boolean informe = Boolean.TRUE.equals(request.informePrealablement());
        boolean scolarite = Boolean.TRUE.equals(request.scolariteImpactee());
        boolean modifDvh = Boolean.TRUE.equals(request.modificationDvhDemandee());

        ChangementResidenceResult result;
        try {
            result = ChangementResidenceCalculator.compute(
                    request.dateChangementPrevu(),
                    request.distanceKm(),
                    request.raisonChangement(),
                    consentement,
                    informe,
                    request.delaiInformationJours(),
                    request.modeResidenceActuel(),
                    request.ageEnfants(),
                    scolarite,
                    modifDvh);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        ChangementResidenceAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    ChangementResidenceAnalysis a = new ChangementResidenceAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDateChangementPrevu(result.dateChangementPrevu());
        entity.setDistanceKm(result.distanceKm());
        entity.setRaisonChangement(result.raisonChangement());
        entity.setConsentementAutreParent(result.consentementAutreParent());
        entity.setInformePrealablement(result.informePrealablement());
        entity.setDelaiInformationJours(result.delaiInformationJours());
        entity.setModeResidenceActuel(result.modeResidenceActuel());
        entity.setScolariteImpactee(result.scolariteImpactee());
        entity.setModificationDvhDemandee(result.modificationDvhDemandee());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public ChangementResidenceResponse get(UUID caseFileId, OidcUser oidcUser,
                                           Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        ChangementResidenceAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de changement de résidence trouvée pour ce dossier"));
        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;
        return toResponse(caseFileId, country, deserialize(entity.getResultData()));
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        return currentUserResolver.resolve(oidcUser,
                OAuthProviderResolver.resolve(principal), principal);
    }

    private CaseFile resolveCaseFile(UUID caseFileId, User user) {
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Case file not found"));
        boolean member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(cf.getWorkspace().getId()))
                .orElse(false);
        if (!member) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }
        if (!"DROIT_FAMILLE".equals(cf.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit de la famille");
        }
        return cf;
    }

    private String serialize(Object o) {
        try { return objectMapper.writeValueAsString(o); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de sérialisation");
        }
    }

    private ChangementResidenceResult deserialize(String json) {
        try { return objectMapper.readValue(json, ChangementResidenceResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private ChangementResidenceResponse toResponse(UUID caseFileId, String country,
                                                   ChangementResidenceResult r) {
        return new ChangementResidenceResponse(
                caseFileId,
                r.dateChangementPrevu(),
                r.distanceKm(),
                r.raisonChangement(),
                r.consentementAutreParent(),
                r.informePrealablement(),
                r.delaiInformationJours(),
                r.modeResidenceActuel(),
                r.ageEnfants() != null ? r.ageEnfants() : List.of(),
                r.scolariteImpactee(),
                r.modificationDvhDemandee(),
                r.scoreAcceptabilite(),
                r.verdictProbabiliteAcceptation(),
                r.obligationInformationRespectee(),
                r.expertisePsyEnfantRecommandee(),
                r.delaiPreavisLegalOk(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : List.of(),
                country
        );
    }
}
