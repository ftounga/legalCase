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
 * SF-221-04 : service applicatif de l'outil détention en centre fermé + requête de mise
 * en liberté BE (Loi 15/12/1980 art. 7 al. 3 / 27 / 29 / 74/5 ; AR 02/08/2002 ;
 * chambre du conseil art. 71 et s.).
 *
 * <p>Gates :
 * <ul>
 *   <li>workspace.country = BELGIQUE (sinon 400 — outil BE-only) ;</li>
 *   <li>caseFile.legalDomain = DROIT_IMMIGRATION (sinon 400) ;</li>
 *   <li>champ invalide (date absente / future, booléen manquant, conditions
 *       conditionnelles) → 400 ;</li>
 *   <li>isolation workspace par {@code WorkspaceMemberRepository} (sinon 404).</li>
 * </ul>
 */
@Service
public class DetentionCentreFermeBeService {

    private final DetentionCentreFermeBeRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public DetentionCentreFermeBeService(
            DetentionCentreFermeBeRepository repository,
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
    public DetentionCentreFermeBeResponse calculate(UUID caseFileId,
                                                    DetentionCentreFermeBeRequest request,
                                                    OidcUser oidcUser,
                                                    Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Détention en centre fermé + requête de mise en liberté : outil BELGIQUE "
                            + "uniquement — pour la France voir les outils OQTF / placement en CRA.");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        DetentionCentreFermeBeResult result;
        try {
            result = DetentionCentreFermeBeCalculator.compute(
                    request.dateDebutDetention(),
                    request.baseLegaleDetention(),
                    request.prolongationNotifiee(),
                    request.dateProlongation(),
                    request.requeteMiseEnLiberteDeposee(),
                    request.dateNotificationDecisionDetention());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        DetentionCentreFermeBeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    DetentionCentreFermeBeAnalysis a = new DetentionCentreFermeBeAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDateDebutDetention(result.dateDebutDetention());
        entity.setBaseLegaleDetention(result.baseLegaleDetention());
        entity.setProlongationNotifiee(result.prolongationNotifiee());
        entity.setDateProlongation(result.dateProlongation());
        entity.setRequeteMiseEnLiberteDeposee(result.requeteMiseEnLiberteDeposee());
        entity.setDateNotificationDecisionDetention(result.dateNotificationDecisionDetention());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, result);
    }

    @Transactional(readOnly = true)
    public DetentionCentreFermeBeResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        DetentionCentreFermeBeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse détention en centre fermé BE trouvée pour ce dossier"));
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
        if (!member) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }
        if (!"DROIT_IMMIGRATION".equals(cf.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit de l'immigration");
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

    private DetentionCentreFermeBeResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, DetentionCentreFermeBeResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private DetentionCentreFermeBeResponse toResponse(UUID caseFileId, DetentionCentreFermeBeResult r) {
        return new DetentionCentreFermeBeResponse(
                caseFileId,
                r.dateDebutDetention(),
                r.baseLegaleDetention(),
                r.prolongationNotifiee(),
                r.dateProlongation(),
                r.requeteMiseEnLiberteDeposee(),
                r.dateNotificationDecisionDetention(),
                r.verdict(),
                r.dureeDetentionJours(),
                r.dateLimiteRequete(),
                r.joursRestantsRequete(),
                r.basesJuridiques() != null ? r.basesJuridiques() : java.util.List.of(),
                r.messages() != null ? r.messages() : java.util.List.of());
    }
}
