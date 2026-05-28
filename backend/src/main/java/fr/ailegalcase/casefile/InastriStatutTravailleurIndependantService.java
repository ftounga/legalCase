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
 * SF-219-27 : service orchestrant l'analyse <i>INASTI — statut
 * travailleur indépendant et qualification salarié / indépendant</i>
 * (Loi du 27/06/1969 + AR n° 38 du 27/07/1967 + Loi-programme I du
 * 27/12/2006 art. 328 à 333 « doctrine Bart Buysse » + art. 337/2
 * critères sectoriels).
 *
 * <p>Gate <b>BELGIQUE strict</b> (404 côté FR — réponse indistinguable
 * d'un dossier inconnu, pattern miroir SF-219-06/08/10/12/14/15/16/
 * 17/18/19/20/21/22/23/24/25/26) + isolation workspace standard +
 * gate domaine {@code DROIT_DU_TRAVAIL} (400) + persistance JSON
 * snapshot.</p>
 *
 * <p>La France a un régime distinct (présomption de salariat
 * art. L. 8221-6 Code du travail français — présomption générale
 * d'absence de salariat pour les inscrits BCE / RCS, renversement
 * par preuve de subordination juridique permanente ; et inversement
 * art. L. 8221-6-1 pour les personnes effectuant des prestations
 * dans des conditions du salariat ; le faisceau d'indices français
 * n'est pas codifié de la même manière que les 4 critères
 * généraux art. 333 belges). Restitution séparée par outil — pas
 * de tentative d'harmonisation FR/BE.</p>
 *
 * <p>Le checker sous-jacent
 * ({@link InastriStatutTravailleurIndependantChecker}) est une
 * fonction pure indépendante du contexte HTTP / persistance.</p>
 */
@Service
public class InastriStatutTravailleurIndependantService {

    private final InastriStatutTravailleurIndependantAnalysisRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public InastriStatutTravailleurIndependantService(
            InastriStatutTravailleurIndependantAnalysisRepository repository,
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
    public InastriStatutTravailleurIndependantResponse analyze(
            UUID caseFileId,
            InastriStatutTravailleurIndependantRequest request,
            OidcUser oidcUser,
            Principal principal) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Corps de requête requis");
        }
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;
        // Gate BE-only strict : 404 (et pas 400) pour ne pas divulguer
        // l'existence de l'outil côté FR — pattern miroir
        // SF-219-06/08/10/12/14/15/16/17/18/19/20/21/22/23/24/25/26.
        // La France a un régime distinct (présomption art. L. 8221-6 et
        // L. 8221-6-1 Code du travail, faisceau d'indices non codifié
        // selon les 4 critères généraux art. 333 belges). Restitution
        // séparée par outil.
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Case file not found");
        }

        InastriStatutTravailleurIndependantResult result;
        try {
            result = InastriStatutTravailleurIndependantChecker
                    .analyze(request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    e.getMessage());
        }

        InastriStatutTravailleurIndependantResponse response =
                toResponse(caseFileId, result);

        InastriStatutTravailleurIndependantAnalysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseGet(() -> {
                            InastriStatutTravailleurIndependantAnalysis a =
                                    new InastriStatutTravailleurIndependantAnalysis();
                            a.setCaseFile(caseFile);
                            return a;
                        });
        entity.setResultData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public InastriStatutTravailleurIndependantResponse get(
            UUID caseFileId,
            OidcUser oidcUser,
            Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Case file not found");
        }

        InastriStatutTravailleurIndependantAnalysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Aucune analyse INASTI statut travailleur"
                                        + " indépendant trouvée pour ce dossier"));
        return deserialize(entity.getResultData());
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        return currentUserResolver.resolve(oidcUser,
                OAuthProviderResolver.resolve(principal), principal);
    }

    private CaseFile resolveCaseFile(UUID caseFileId, User user) {
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Case file not found"));
        boolean member = workspaceMemberRepository
                .findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId()
                        .equals(cf.getWorkspace().getId()))
                .orElse(false);
        if (!member) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Case file not found");
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
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de sérialisation");
        }
    }

    private InastriStatutTravailleurIndependantResponse deserialize(
            String json) {
        try {
            return objectMapper.readValue(json,
                    InastriStatutTravailleurIndependantResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private InastriStatutTravailleurIndependantResponse toResponse(
            UUID caseFileId,
            InastriStatutTravailleurIndependantResult r) {
        return new InastriStatutTravailleurIndependantResponse(
                caseFileId,
                r.volonteParties(),
                r.liberteOrganisationTemps(),
                r.liberteOrganisationTravail(),
                r.controleHierarchique(),
                r.secteur(),
                r.nbCriteresSectorielsSalarie(),
                r.statutDeclare(),
                r.dimonaPresente(),
                r.autreClientPresent(),
                r.verdict(),
                r.scoreCriteresGenerauxIndependant(),
                r.scoreCriteresGenerauxSalarie(),
                r.presumptionSectorielleApplicable(),
                r.presumptionSectorielleDeclenchee(),
                r.presumptionGeneraleSalariatMobilisable(),
                r.requalificationSalariatRecommandee(),
                r.dimonaCoherenteAvecStatutDeclare(),
                r.monoClientIndiceSalariat(),
                r.raison(),
                r.synthese(),
                r.baseJuridique(),
                r.avertissement());
    }
}
