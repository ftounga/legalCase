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
 * SF-FA-18-09 : service orchestrant l'analyse de recevabilité d'une
 * adoption (FR — DROIT_FAMILLE — art. 343-370-2 Cciv).
 */
@Service
public class AdoptionService {

    private final AdoptionRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public AdoptionService(AdoptionRepository repository,
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
    public AdoptionResponse calculate(UUID caseFileId,
                                      AdoptionRequest request,
                                      OidcUser oidcUser,
                                      Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.formeAdoption() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Forme d'adoption requise (PLENIERE ou SIMPLE)");
        }
        if (request.ageAdoptant() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Âge de l'adoptant requis");
        }
        if (request.ageAdopte() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Âge de l'adopté requis");
        }
        if (request.ageAdoptant() < 0 || request.ageAdopte() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Âges doivent être positifs");
        }

        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace().getCountry();

        AdoptionResult result;
        try {
            result = AdoptionCalculator.compute(
                    request.formeAdoption(),
                    request.ageAdoptant(),
                    request.ageAdopte(),
                    request.consentementParents(),
                    request.consentementAdopte(),
                    request.consentementConjointAdoptant(),
                    request.enquetes(),
                    request.placement6mois(),
                    request.pupilleEtat(),
                    request.adoptantMarie(),
                    country
            );
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        AdoptionAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    AdoptionAnalysis a = new AdoptionAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setFormeAdoption(result.formeAdoption());
        entity.setAgeAdoptant(result.ageAdoptant());
        entity.setAgeAdopte(result.ageAdopte());
        entity.setConsentementParents(result.consentementParents());
        entity.setConsentementAdopte(result.consentementAdopte());
        entity.setConsentementConjointAdoptant(result.consentementConjointAdoptant());
        entity.setEnquetes(result.enquetes());
        entity.setPlacement6mois(result.placement6mois());
        entity.setPupilleEtat(result.pupilleEtat());
        entity.setCountry(result.country());
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, result);
    }

    @Transactional(readOnly = true)
    public AdoptionResponse get(UUID caseFileId,
                                OidcUser oidcUser,
                                Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        AdoptionAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse Adoption trouvée pour ce dossier"));
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
        if (!"DROIT_FAMILLE".equals(cf.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit de la famille");
        }
        return cf;
    }

    private String serialize(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de sérialisation");
        }
    }

    private AdoptionResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, AdoptionResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private AdoptionResponse toResponse(UUID caseFileId, AdoptionResult r) {
        return new AdoptionResponse(
                caseFileId,
                r.formeAdoption(),
                r.formeRecommandee(),
                r.verdictRecevabilite(),
                r.ageAdoptant(),
                r.ageAdopte(),
                r.differenceAgeAns(),
                r.criteresNonRemplis() != null ? r.criteresNonRemplis() : List.of(),
                r.delaiInstructionMois(),
                r.documentsRequis() != null ? r.documentsRequis() : List.of(),
                r.risqueRefus() != null ? r.risqueRefus() : List.of(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : List.of(),
                r.country()
        );
    }
}
