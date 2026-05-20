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
 * SF-207-05 : service orchestrant l'analyse d'éligibilité au référé devant
 * le président du tribunal du travail belge — gate <b>BELGIQUE strict</b>
 * (404 côté FR pour préserver l'isolation BE-only) + isolation workspace
 * standard + validation des dates.
 *
 * <p>Pattern mirroré de {@link C4OnemChecklistService} (SF-207-02) et
 * {@link AtFedrisDeclarationService} (SF-207-04) — gate, persistance JSON,
 * sérialisation Jackson. Le calculateur sous-jacent
 * ({@link RefereTribunalTravailBeCalculator}) est une fonction pure
 * indépendante du contexte HTTP / persistance.</p>
 */
@Service
public class RefereTribunalTravailBeService {

    /** Fuseau horaire belge — cohérent avec les autres outils BE. */
    private static final ZoneId ZONE_BRUSSELS = ZoneId.of("Europe/Brussels");

    private final RefereTribunalTravailBeRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public RefereTribunalTravailBeService(RefereTribunalTravailBeRepository repository,
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
    public RefereTribunalTravailBeResponse calculate(UUID caseFileId,
                                                     RefereTribunalTravailBeRequest request,
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

        RefereTribunalTravailBeResult result;
        try {
            result = RefereTribunalTravailBeCalculator.compute(request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        RefereTribunalTravailBeResponse response = toResponse(caseFileId, country, result);

        RefereTribunalTravailBeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    RefereTribunalTravailBeAnalysis a = new RefereTribunalTravailBeAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setResultData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public RefereTribunalTravailBeResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }

        RefereTribunalTravailBeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse référé tribunal du travail BE trouvée pour ce dossier"));
        return deserialize(entity.getResultData());
    }

    /**
     * Validations métier inter-champs hors portée de Bean Validation :
     * <ul>
     *   <li>{@code dateFaitGenerateur} ne peut être dans le futur (le fait
     *       générateur d'une urgence est par nature passé) ;</li>
     *   <li>{@code dateDemarcheAmiable}, si présente, doit être ≥
     *       {@code dateFaitGenerateur} (la démarche amiable suit le fait
     *       générateur).</li>
     * </ul>
     */
    private void validateDates(RefereTribunalTravailBeRequest request) {
        LocalDate today = LocalDate.now(ZONE_BRUSSELS);
        if (request.dateFaitGenerateur() != null && request.dateFaitGenerateur().isAfter(today)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "dateFaitGenerateur ne peut être dans le futur");
        }
        if (request.dateDemarcheAmiable() != null
                && request.dateFaitGenerateur() != null
                && request.dateDemarcheAmiable().isBefore(request.dateFaitGenerateur())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "dateDemarcheAmiable ne peut être antérieure à dateFaitGenerateur");
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

    private RefereTribunalTravailBeResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, RefereTribunalTravailBeResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private RefereTribunalTravailBeResponse toResponse(UUID caseFileId, String country,
                                                       RefereTribunalTravailBeResult r) {
        return new RefereTribunalTravailBeResponse(
                caseFileId,
                r.motifUrgence(),
                r.motifUrgenceDescription(),
                r.dateFaitGenerateur(),
                r.dateDemarcheAmiable(),
                r.preuveUrgenceJointe(),
                r.mesureProvisoireDemandee(),
                r.perilEnDemeure(),
                r.competenceTerritorialeIdentifiee(),
                r.verdict(),
                r.conditionsNonRemplies(),
                r.scoreConditions(),
                r.requeteSquelette(),
                r.baseJuridique(),
                r.etapeSuivante(),
                country);
    }
}
