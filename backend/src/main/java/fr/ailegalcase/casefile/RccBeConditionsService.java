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
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

/**
 * SF-207-06 : service orchestrant l'analyse RCC BE — gate
 * <b>BELGIQUE strict</b> (404 côté FR pour préserver l'isolation BE-only) +
 * isolation workspace standard + validations métier inter-champs (date
 * naissance future, > 100 ans, date licenciement &lt; date naissance).
 *
 * <p>Pattern mirroré de {@link RefereTribunalTravailBeService} (SF-207-05) —
 * gate, persistance JSON, sérialisation Jackson. Le calculateur sous-jacent
 * ({@link RccBeConditionsCalculator}) est une fonction pure indépendante du
 * contexte HTTP / persistance.</p>
 */
@Service
public class RccBeConditionsService {

    /** Fuseau horaire belge — cohérent avec les autres outils BE. */
    private static final ZoneId ZONE_BRUSSELS = ZoneId.of("Europe/Brussels");

    /** Borne haute plausible d'un âge humain (à la date envisagée). */
    private static final int AGE_MAX_PLAUSIBLE = 100;

    private final RccBeConditionsRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public RccBeConditionsService(RccBeConditionsRepository repository,
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
    public RccBeConditionsResponse calculate(UUID caseFileId,
                                             RccBeConditionsRequest request,
                                             OidcUser oidcUser,
                                             Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        // Gate BE-only strict : 404 (et pas 400) pour ne pas divulguer l'existence
        // de l'outil côté FR — réponse indistinguable d'un dossier inconnu.
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }

        // Validations métier inter-champs (au-delà des Bean Validation).
        validateDates(request);

        RccBeConditionsResult result;
        try {
            result = RccBeConditionsCalculator.compute(request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        RccBeConditionsResponse response = toResponse(caseFileId, country, result);

        RccBeConditionsAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    RccBeConditionsAnalysis a = new RccBeConditionsAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setResultData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public RccBeConditionsResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }

        RccBeConditionsAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse RCC BE trouvée pour ce dossier"));
        return deserialize(entity.getResultData());
    }

    /**
     * Validations métier inter-champs hors portée de Bean Validation :
     * <ul>
     *   <li>{@code dateNaissance} ne peut être dans le futur ;</li>
     *   <li>{@code dateNaissance} ne peut correspondre à un âge &gt; 100 ans
     *       (donnée manifestement erronée) ;</li>
     *   <li>{@code dateLicenciementEnvisagee}, si présente, doit être ≥
     *       {@code dateNaissance}.</li>
     * </ul>
     */
    private void validateDates(RccBeConditionsRequest request) {
        LocalDate today = LocalDate.now(ZONE_BRUSSELS);
        LocalDate naissance = request.dateNaissance();
        if (naissance != null) {
            if (naissance.isAfter(today)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "dateNaissance ne peut être dans le futur");
            }
            if (naissance.isBefore(today.minusYears(AGE_MAX_PLAUSIBLE))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "dateNaissance correspond à un âge supérieur à 100 ans");
            }
        }
        LocalDate licenciement = request.dateLicenciementEnvisagee();
        if (licenciement != null && naissance != null && licenciement.isBefore(naissance)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "dateLicenciementEnvisagee ne peut être antérieure à dateNaissance");
        }
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

    private RccBeConditionsResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, RccBeConditionsResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private RccBeConditionsResponse toResponse(UUID caseFileId, String country,
                                               RccBeConditionsResult r) {
        return new RccBeConditionsResponse(
                caseFileId,
                r.dateNaissance(),
                r.anneesCarriereProfessionnelle(),
                r.metierLourd(),
                r.longueCarriere(),
                r.entrepriseEnDifficulte(),
                r.dateLicenciementEnvisagee(),
                r.verdict(),
                r.regimeApplicable(),
                r.regimesEligibles(),
                r.ageALaDateLicenciement(),
                r.anneesCarriereCalculees(),
                r.conditionsManquantes(),
                r.baseJuridique(),
                r.formuleCalcul(),
                country);
    }
}
