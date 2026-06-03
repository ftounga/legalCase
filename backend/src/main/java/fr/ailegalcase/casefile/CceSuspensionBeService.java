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
 * SF-221-05 : service applicatif de l'outil recours CCE en suspension ordinaire BE
 * (art. 39/82 Loi 15/12/1980 ; loi 15/09/2006).
 *
 * <p>Gates :
 * <ul>
 *   <li>workspace.country = BELGIQUE (sinon 400 — outil BE-only) ;</li>
 *   <li>caseFile.legalDomain = DROIT_IMMIGRATION (sinon 400) ;</li>
 *   <li>champ invalide (date absente / future, booléen manquant) → 400 ;</li>
 *   <li>isolation workspace par {@code WorkspaceMemberRepository} (sinon 404).</li>
 * </ul>
 */
@Service
public class CceSuspensionBeService {

    private final CceSuspensionBeRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public CceSuspensionBeService(
            CceSuspensionBeRepository repository,
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
    public CceSuspensionBeResponse calculate(UUID caseFileId,
                                             CceSuspensionBeRequest request,
                                             OidcUser oidcUser,
                                             Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Recours CCE en suspension : outil BELGIQUE uniquement — pour la France "
                            + "voir les outils OQTF / référé suspension TA.");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        CceSuspensionBeResult result;
        try {
            result = CceSuspensionBeCalculator.compute(
                    request.dateNotificationDecision(),
                    request.recoursAnnulationIntroduit(),
                    request.urgenceInvocable(),
                    request.risquePrejudiceGraveDifficilementReparable(),
                    request.mesureEloignementImminente());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        CceSuspensionBeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    CceSuspensionBeAnalysis a = new CceSuspensionBeAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDateNotificationDecision(result.dateNotificationDecision());
        entity.setRecoursAnnulationIntroduit(result.recoursAnnulationIntroduit());
        entity.setUrgenceInvocable(result.urgenceInvocable());
        entity.setRisquePrejudiceGraveDifficilementReparable(
                result.risquePrejudiceGraveDifficilementReparable());
        entity.setMesureEloignementImminente(result.mesureEloignementImminente());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, result);
    }

    @Transactional(readOnly = true)
    public CceSuspensionBeResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        CceSuspensionBeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse recours CCE suspension BE trouvée pour ce dossier"));
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

    private CceSuspensionBeResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, CceSuspensionBeResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private CceSuspensionBeResponse toResponse(UUID caseFileId, CceSuspensionBeResult r) {
        return new CceSuspensionBeResponse(
                caseFileId,
                r.dateNotificationDecision(),
                r.recoursAnnulationIntroduit(),
                r.urgenceInvocable(),
                r.risquePrejudiceGraveDifficilementReparable(),
                r.mesureEloignementImminente(),
                r.verdict(),
                r.joursDepuisNotification(),
                r.renvoiOutil(),
                r.basesJuridiques() != null ? r.basesJuridiques() : java.util.List.of(),
                r.messages() != null ? r.messages() : java.util.List.of());
    }
}
