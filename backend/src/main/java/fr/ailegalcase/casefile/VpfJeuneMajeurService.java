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
import java.util.UUID;

/**
 * SF-220-03 : service de l'outil décisionnel « VPF jeune majeur L.423-22 »
 * (F-IM-49-vpf-jeune-majeur-l42322-fr). Outil single-country FR.
 *
 * <p>Pattern miroir de {@link RegimeMayotteService} (SF-220-02).</p>
 */
@Service
public class VpfJeuneMajeurService {

    private static final int AGE_MIN_VALIDE = 1;
    private static final int AGE_MAX_VALIDE = 30;

    private final VpfJeuneMajeurRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public VpfJeuneMajeurService(VpfJeuneMajeurRepository repository,
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
    public VpfJeuneMajeurResponse analyze(UUID caseFileId, VpfJeuneMajeurRequest request,
                                          OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "VPF jeune majeur — outil FRANCE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.age() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le champ age est requis");
        }
        int age = request.age();
        if (age < AGE_MIN_VALIDE || age > AGE_MAX_VALIDE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le champ age doit être compris entre " + AGE_MIN_VALIDE + " et " + AGE_MAX_VALIDE);
        }
        if (request.ageEntreeAse() != null
                && (request.ageEntreeAse() < 0 || request.ageEntreeAse() > AGE_MAX_VALIDE)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le champ ageEntreeAse doit être compris entre 0 et " + AGE_MAX_VALIDE);
        }
        if (request.ancienneteMoisPriseEnCharge() != null
                && request.ancienneteMoisPriseEnCharge() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le champ ancienneteMoisPriseEnCharge doit être positif ou nul");
        }

        boolean entreMineur = Boolean.TRUE.equals(request.entreMineur());
        boolean priseEnChargeAse = Boolean.TRUE.equals(request.priseEnChargeAse());
        boolean scolariseOuFormation = Boolean.TRUE.equals(request.scolariseOuFormation());
        boolean caractereReelEtSerieux = Boolean.TRUE.equals(request.caractereReelEtSerieuxFormation());
        boolean avisStructureFavorable = Boolean.TRUE.equals(request.avisStructureFavorable());
        boolean absenceLienFamillePays = Boolean.TRUE.equals(request.absenceLienFamillePays());

        VpfJeuneMajeurResult result = VpfJeuneMajeurAnalyzer.analyze(
                age,
                entreMineur,
                request.ageEntreeAse(),
                priseEnChargeAse,
                request.ancienneteMoisPriseEnCharge(),
                scolariseOuFormation,
                caractereReelEtSerieux,
                avisStructureFavorable,
                absenceLienFamillePays);

        VpfJeuneMajeurAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    VpfJeuneMajeurAnalysis a = new VpfJeuneMajeurAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setAge(result.age());
        entity.setEntreMineur(result.entreMineur());
        entity.setDateEntreeFrance(request.dateEntreeFrance());
        entity.setAgeEntreeAse(result.ageEntreeAse());
        entity.setPriseEnChargeAse(result.priseEnChargeAse());
        entity.setDateDebutPriseEnCharge(request.dateDebutPriseEnCharge());
        entity.setAncienneteMoisPriseEnCharge(result.ancienneteMoisPriseEnCharge());
        entity.setScolariseOuFormation(result.scolariseOuFormation());
        entity.setCaractereReelEtSerieuxFormation(result.caractereReelEtSerieuxFormation());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, request.dateEntreeFrance(),
                request.dateDebutPriseEnCharge(), result);
    }

    @Transactional(readOnly = true)
    public VpfJeuneMajeurResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        VpfJeuneMajeurAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse VPF jeune majeur trouvée pour ce dossier"));
        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        return toResponse(caseFileId, country, entity.getDateEntreeFrance(),
                entity.getDateDebutPriseEnCharge(), deserialize(entity.getResultData()));
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
        if (!"DROIT_IMMIGRATION".equals(cf.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit de l'immigration");
        }
        return cf;
    }

    private String serialize(Object o) {
        try { return objectMapper.writeValueAsString(o); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation");
        }
    }

    private VpfJeuneMajeurResult deserialize(String json) {
        try { return objectMapper.readValue(json, VpfJeuneMajeurResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private VpfJeuneMajeurResponse toResponse(UUID caseFileId, String country,
                                              LocalDate dateEntreeFrance, LocalDate dateDebutPriseEnCharge,
                                              VpfJeuneMajeurResult r) {
        return new VpfJeuneMajeurResponse(
                caseFileId,
                r.age(),
                r.entreMineur(),
                dateEntreeFrance,
                r.ageEntreeAse(),
                r.priseEnChargeAse(),
                dateDebutPriseEnCharge,
                r.ancienneteMoisPriseEnCharge(),
                r.scolariseOuFormation(),
                r.caractereReelEtSerieuxFormation(),
                country,
                r.eligibilite(),
                r.ancienneteRequiseMois(),
                r.criteresManquants(),
                r.basesJuridiques(),
                r.messages());
    }
}
