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
 * SF-207-08 : service orchestrant l'analyse outplacement obligatoire 45+
 * BE — gate <b>BELGIQUE strict</b> (404 côté FR pour préserver l'isolation
 * BE-only) + isolation workspace standard + validations métier inter-champs
 * (cohérence des dates, salarié majeur, validations conditionnelles de
 * l'offre).
 *
 * <p>Pattern mirroré de {@link C4OnemChecklistService} (SF-207-02) — gate,
 * persistance JSON, sérialisation Jackson. Le calculateur sous-jacent
 * ({@link OutplacementBeCalculator}) est une fonction pure indépendante du
 * contexte HTTP / persistance.</p>
 */
@Service
public class OutplacementBeService {

    /** Fuseau horaire belge — cohérent avec les autres outils BE. */
    private static final ZoneId ZONE_BRUSSELS = ZoneId.of("Europe/Brussels");

    /** Borne haute plausible d'un âge humain. */
    private static final int AGE_MAX_PLAUSIBLE = 100;

    /** Âge minimum légal du salariat — un licenciement avant 15 ans relève d'une saisie erronée. */
    private static final int AGE_MINIMUM_SALARIE = 15;

    private final OutplacementBeRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public OutplacementBeService(OutplacementBeRepository repository,
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
    public OutplacementBeResponse calculate(UUID caseFileId,
                                            OutplacementBeRequest request,
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
        validateDatesAndConditionals(request);

        OutplacementBeResult result;
        try {
            result = OutplacementBeCalculator.compute(request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        OutplacementBeResponse response = toResponse(caseFileId, country, result);

        OutplacementBeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    OutplacementBeAnalysis a = new OutplacementBeAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setResultData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public OutplacementBeResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }

        OutplacementBeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse outplacement BE trouvée pour ce dossier"));
        return deserialize(entity.getResultData());
    }

    /**
     * Validations métier inter-champs hors portée de Bean Validation :
     * <ul>
     *   <li>{@code dateNaissanceSalarie} ne peut être dans le futur ;</li>
     *   <li>{@code dateNaissanceSalarie} ne peut correspondre à un âge &gt; 100 ans ;</li>
     *   <li>{@code dateLicenciement} ne peut être dans le futur ;</li>
     *   <li>{@code dateLicenciement} doit être ≥ {@code dateNaissance + 15 ans}
     *       (âge minimum de salariat — toute date antérieure = saisie erronée) ;</li>
     *   <li>Si {@code offreOutplacementRecue=true}, alors {@code dateOffreOutplacement}
     *       et {@code offreConformeCCT82} doivent être fournis.</li>
     * </ul>
     */
    private void validateDatesAndConditionals(OutplacementBeRequest request) {
        LocalDate today = LocalDate.now(ZONE_BRUSSELS);
        LocalDate naissance = request.dateNaissanceSalarie();
        LocalDate licenciement = request.dateLicenciement();

        if (naissance != null) {
            if (naissance.isAfter(today)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "dateNaissanceSalarie ne peut être dans le futur");
            }
            if (naissance.isBefore(today.minusYears(AGE_MAX_PLAUSIBLE))) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "dateNaissanceSalarie correspond à un âge supérieur à 100 ans");
            }
        }
        if (licenciement != null) {
            if (licenciement.isAfter(today)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "dateLicenciement ne peut être dans le futur");
            }
            if (naissance != null) {
                LocalDate seuilMin = naissance.plusYears(AGE_MINIMUM_SALARIE);
                if (licenciement.isBefore(seuilMin)) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "dateLicenciement doit être ≥ dateNaissanceSalarie + 15 ans "
                                    + "(âge minimum de salariat)");
                }
            }
        }

        // Validations conditionnelles : offre reçue → dateOffre + conformité requis.
        if (Boolean.TRUE.equals(request.offreOutplacementRecue())) {
            if (request.dateOffreOutplacement() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "dateOffreOutplacement est requise quand offreOutplacementRecue=true");
            }
            if (request.offreConformeCCT82() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "offreConformeCCT82 est requise quand offreOutplacementRecue=true");
            }
            if (licenciement != null && request.dateOffreOutplacement().isBefore(licenciement)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "dateOffreOutplacement ne peut être antérieure à dateLicenciement");
            }
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

    private OutplacementBeResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json, OutplacementBeResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private OutplacementBeResponse toResponse(UUID caseFileId, String country,
                                              OutplacementBeResult r) {
        return new OutplacementBeResponse(
                caseFileId,
                r.dateLicenciement(),
                r.dateNaissanceSalarie(),
                r.ancienneteAnnees(),
                r.motifLicenciement(),
                r.contratTempsPlein(),
                r.offreOutplacementRecue(),
                r.dateOffreOutplacement(),
                r.offreConformeCCT82(),
                r.salarieAcceptantOffre(),
                r.verdict(),
                r.ageALaDateLicenciement(),
                r.obligationEmployeurApplicable(),
                r.raisonNonObligation(),
                r.sanctionEmployeurEuros(),
                r.sanctionSalarieRange(),
                r.dateLimiteOffre(),
                r.delaiOffreRespecte(),
                r.etapeSuivante(),
                r.baseJuridique(),
                r.formuleCalcul(),
                country);
    }
}
