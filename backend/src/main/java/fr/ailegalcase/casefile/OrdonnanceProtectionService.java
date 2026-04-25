package fr.ailegalcase.casefile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
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
 * SF-FA-14-01 : service orchestrant l'analyse d'ordonnance de protection FR
 * (art. 515-9 à 515-13 Cciv + Loi 30/07/2020). Gate FRANCE + DROIT_FAMILLE
 * strict.
 */
@Service
public class OrdonnanceProtectionService {

    private final OrdonnanceProtectionRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public OrdonnanceProtectionService(OrdonnanceProtectionRepository repository,
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
    public OrdonnanceProtectionResponse calculate(UUID caseFileId,
                                                  OrdonnanceProtectionRequest request,
                                                  OidcUser oidcUser,
                                                  Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ordonnance de protection FR uniquement (art. 515-9 Cciv) — équivalent BE = art. 1253ter CJ");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.violencesAlleguees() == null || request.violencesAlleguees().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "violencesAlleguees est requis et non vide");
        }
        if (request.dangerImmediat() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dangerImmediat est requis");
        }
        if (request.presenceEnfants() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "presenceEnfants est requis");
        }
        if (request.logementCommun() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "logementCommun est requis");
        }
        if (request.victimeFinanciairementDependante() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "victimeFinanciairementDependante est requis");
        }
        if (request.demandeurDejaProtege() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "demandeurDejaProtege est requis");
        }

        OrdonnanceProtectionResult result;
        try {
            result = OrdonnanceProtectionCalculator.compute(
                    request.dateRequete(),
                    request.violencesAlleguees(),
                    request.preuvesViolences() != null ? request.preuvesViolences() : Collections.emptyList(),
                    request.dangerImmediat(),
                    request.presenceEnfants(),
                    request.ageEnfants(),
                    request.logementCommun(),
                    request.victimeFinanciairementDependante(),
                    request.demandeurDejaProtege(),
                    request.demandeMesures() != null ? request.demandeMesures() : Collections.emptyList());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        OrdonnanceProtectionAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    OrdonnanceProtectionAnalysis a = new OrdonnanceProtectionAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDateRequete(result.dateRequete());
        entity.setViolencesAlleguees(serialize(result.violencesAlleguees()));
        entity.setPreuvesViolences(serialize(result.preuvesViolences()));
        entity.setDangerImmediat(result.dangerImmediat());
        entity.setPresenceEnfants(result.presenceEnfants());
        entity.setAgeEnfants(result.ageEnfants() != null && !result.ageEnfants().isEmpty()
                ? serialize(result.ageEnfants()) : null);
        entity.setLogementCommun(result.logementCommun());
        entity.setVictimeFinanciairementDependante(result.victimeFinanciairementDependante());
        entity.setDemandeurDejaProtege(result.demandeurDejaProtege());
        entity.setDemandeMesures(serialize(result.demandeMesures()));
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public OrdonnanceProtectionResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        OrdonnanceProtectionAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse d'ordonnance de protection trouvée pour ce dossier"));
        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        return toResponse(caseFileId, country, deserializeResult(entity.getResultData()));
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
        if (!"DROIT_FAMILLE".equals(cf.getLegalDomain()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit de la famille");
        return cf;
    }

    private String serialize(Object o) {
        try { return objectMapper.writeValueAsString(o); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation");
        }
    }

    private OrdonnanceProtectionResult deserializeResult(String json) {
        try { return objectMapper.readValue(json, OrdonnanceProtectionResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private OrdonnanceProtectionResponse toResponse(UUID caseFileId, String country,
                                                    OrdonnanceProtectionResult r) {
        return new OrdonnanceProtectionResponse(
                caseFileId,
                r.dateRequete(),
                r.violencesAlleguees() != null ? r.violencesAlleguees() : List.of(),
                r.preuvesViolences() != null ? r.preuvesViolences() : List.of(),
                r.dangerImmediat(),
                r.presenceEnfants(),
                r.ageEnfants() != null ? r.ageEnfants() : List.of(),
                r.logementCommun(),
                r.victimeFinanciairementDependante(),
                r.demandeurDejaProtege(),
                r.demandeMesures() != null ? r.demandeMesures() : List.of(),
                r.scoreVraisemblance(),
                r.verdictProbabiliteOctroi(),
                r.mesuresRecommandees() != null ? r.mesuresRecommandees() : List.of(),
                r.delaiTraitementJoursPrevisionnel(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : List.of(),
                country);
    }

    @SuppressWarnings("unused")
    private List<Integer> deserializeAges(String json) {
        if (json == null || json.isBlank()) return Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<List<Integer>>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }
}
