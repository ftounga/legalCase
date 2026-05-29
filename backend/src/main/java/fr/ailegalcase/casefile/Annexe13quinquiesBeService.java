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
 * SF-215-17 : service applicatif de l'outil "Annexe 13quinquies OQT + interdiction
 * d'entrée BE" (art. 74/11 Loi 15/12/1980). Outil <b>BELGIQUE UNIQUEMENT</b>
 * (droit des étrangers).
 *
 * <p>Gates :
 * <ul>
 *   <li>workspace.country = BELGIQUE (sinon 400 — outil BE-only) ;</li>
 *   <li>caseFile.legalDomain = DROIT_IMMIGRATION (sinon 400) ;</li>
 *   <li>isolation workspace par {@code WorkspaceMemberRepository} (sinon 404).</li>
 * </ul>
 */
@Service
public class Annexe13quinquiesBeService {

    private final Annexe13quinquiesBeRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public Annexe13quinquiesBeService(
            Annexe13quinquiesBeRepository repository,
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
    public Annexe13quinquiesBeResponse calculate(UUID caseFileId,
                                                 Annexe13quinquiesBeRequest request,
                                                 OidcUser oidcUser,
                                                 Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Annexe 13quinquies (OQT + interdiction d'entrée) — outil BELGIQUE uniquement "
                            + "(art. 74/11 Loi 15/12/1980).");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        Annexe13quinquiesBeResult result;
        try {
            result = Annexe13quinquiesBeCalculator.compute(
                    request.dateNotificationAnnexe(),
                    request.motifInterdictionEntree(),
                    request.precedentSejour(),
                    request.recoursForme(),
                    request.dateRecours());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        Annexe13quinquiesBeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    Annexe13quinquiesBeAnalysis a = new Annexe13quinquiesBeAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDateNotificationAnnexe(result.dateNotificationAnnexe());
        entity.setMotifInterdictionEntree(result.motifInterdictionEntree());
        entity.setPrecedentSejour(result.precedentSejour());
        entity.setDureeInterdiction(result.dureeInterdiction());
        entity.setRecoursForme(result.recoursForme());
        entity.setDateRecours(result.dateRecours());
        entity.setStatut(result.statutRecours());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, result);
    }

    @Transactional(readOnly = true)
    public Annexe13quinquiesBeResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        Annexe13quinquiesBeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse Annexe 13quinquies BE trouvée pour ce dossier"));
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

    private Annexe13quinquiesBeResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, Annexe13quinquiesBeResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private Annexe13quinquiesBeResponse toResponse(UUID caseFileId, Annexe13quinquiesBeResult r) {
        return new Annexe13quinquiesBeResponse(
                caseFileId,
                r.dateNotificationAnnexe(),
                r.motifInterdictionEntree(),
                r.precedentSejour(),
                r.dureeInterdiction(),
                r.dateFinInterdiction(),
                r.datePossibleLevePrecoce(),
                r.dateLimiteRecoursAnnulation(),
                r.joursRestantsRecours(),
                r.recoursForme(),
                r.dateRecours(),
                r.statutRecours(),
                r.conditionsLevee(),
                r.recommandation(),
                r.baseJuridique()
        );
    }
}
