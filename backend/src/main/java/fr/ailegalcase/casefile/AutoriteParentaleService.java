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
 * SF-FA-19-01 : service applicatif autorité parentale (art. 372-373 Cciv).
 * Gate FR + DROIT_FAMILLE, isolation workspace, persistance JSON.
 */
@Service
public class AutoriteParentaleService {

    private final AutoriteParentaleRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public AutoriteParentaleService(AutoriteParentaleRepository repository,
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
    public AutoriteParentaleResponse calculate(UUID caseFileId,
                                               AutoriteParentaleRequest request,
                                               OidcUser oidcUser,
                                               Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Autorité parentale : procédure propre au droit français "
                            + "(art. 372-373 Cciv). F-FA-19 BE non couverte.");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Corps de requête requis");
        }
        if (request.regimeExerciceActuel() == null || request.regimeExerciceActuel().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "regimeExerciceActuel est requis");
        }
        if (request.regimeExerciceDemande() == null || request.regimeExerciceDemande().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "regimeExerciceDemande est requis");
        }
        if (request.motifChangement() == null || request.motifChangement().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "motifChangement est requis");
        }
        if (request.dateRequete() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "dateRequete est requise");
        }

        boolean danger = Boolean.TRUE.equals(request.dangerCaracterise());
        boolean consentement = Boolean.TRUE.equals(request.consentementAutreParent());
        boolean interference = Boolean.TRUE.equals(request.interferenceVieEnfant());

        AutoriteParentaleResult result;
        try {
            result = AutoriteParentaleCalculator.compute(
                    request.regimeExerciceActuel(),
                    request.regimeExerciceDemande(),
                    request.motifChangement(),
                    danger,
                    request.preuvesProduites(),
                    request.ageEnfants(),
                    consentement,
                    interference,
                    request.dateRequete());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        AutoriteParentaleAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    AutoriteParentaleAnalysis a = new AutoriteParentaleAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setRegimeExerciceActuel(result.regimeExerciceActuel());
        entity.setRegimeExerciceDemande(result.regimeExerciceDemande());
        entity.setMotifChangement(result.motifChangement());
        entity.setDangerCaracterise(result.dangerCaracterise());
        entity.setConsentementAutreParent(result.consentementAutreParent());
        entity.setInterferenceVieEnfant(result.interferenceVieEnfant());
        entity.setDateRequete(result.dateRequete());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public AutoriteParentaleResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        AutoriteParentaleAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse d'autorité parentale trouvée pour ce dossier"));
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

    private AutoriteParentaleResult deserialize(String json) {
        try { return objectMapper.readValue(json, AutoriteParentaleResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private AutoriteParentaleResponse toResponse(UUID caseFileId, String country,
                                                 AutoriteParentaleResult r) {
        return new AutoriteParentaleResponse(
                caseFileId,
                r.regimeExerciceActuel(),
                r.regimeExerciceDemande(),
                r.motifChangement(),
                r.dangerCaracterise(),
                r.preuvesProduites() != null ? r.preuvesProduites() : List.of(),
                r.ageEnfants() != null ? r.ageEnfants() : List.of(),
                r.consentementAutreParent(),
                r.interferenceVieEnfant(),
                r.dateRequete(),
                r.scoreEligibilite(),
                r.verdictProbabiliteAcceptation(),
                r.expertiseRecommandee(),
                r.auditionEnfantsRecommandee(),
                r.baseJuridique(),
                r.formule(),
                r.messages() != null ? r.messages() : List.of(),
                country
        );
    }
}
