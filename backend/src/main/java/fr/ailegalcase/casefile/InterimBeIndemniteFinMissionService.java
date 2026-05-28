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
 * SF-219-15 : service orchestrant le calcul des indemnités de fin de
 * mission intérim BE (Loi 24/07/1987 + CCT n° 322 + CCT n° 322bis +
 * CCT sectorielles + jurisprudence Cass. BE).
 *
 * <p>Gate <b>BELGIQUE strict</b> (404 côté FR — réponse indistinguable
 * d'un dossier inconnu, pattern miroir SF-219-06/08/10/12/14) + isolation
 * workspace standard + gate domaine {@code DROIT_DU_TRAVAIL} (400) +
 * persistance JSON snapshot.</p>
 *
 * <p>Le calculateur sous-jacent ({@link InterimBeIndemniteFinMissionCalculator})
 * est une fonction pure indépendante du contexte HTTP / persistance.</p>
 */
@Service
public class InterimBeIndemniteFinMissionService {

    private final InterimBeIndemniteFinMissionAnalysisRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public InterimBeIndemniteFinMissionService(
            InterimBeIndemniteFinMissionAnalysisRepository repository,
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
    public InterimBeIndemniteFinMissionResponse analyze(
            UUID caseFileId,
            InterimBeIndemniteFinMissionRequest request,
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
        // l'existence de l'outil côté FR — pattern miroir SF-219-06/08/10/12/14.
        // Le régime FR a une IFM forfaitaire 10 % (C. trav. fr.
        // art. L. 1251-32) que la jurisprudence Cass. BE refuse
        // explicitement de transposer (Cass. BE 03/05/2010 et 23/06/2003).
        // Restitution séparée par outil — pas de confusion possible.
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Case file not found");
        }

        InterimBeIndemniteFinMissionResult result;
        try {
            result = InterimBeIndemniteFinMissionCalculator.analyze(request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    e.getMessage());
        }

        InterimBeIndemniteFinMissionResponse response =
                toResponse(caseFileId, result);

        InterimBeIndemniteFinMissionAnalysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseGet(() -> {
                            InterimBeIndemniteFinMissionAnalysis a =
                                    new InterimBeIndemniteFinMissionAnalysis();
                            a.setCaseFile(caseFile);
                            return a;
                        });
        entity.setResultData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public InterimBeIndemniteFinMissionResponse get(
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

        InterimBeIndemniteFinMissionAnalysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Aucune analyse indemnité fin mission"
                                        + " intérim BE trouvée pour ce dossier"));
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

    private InterimBeIndemniteFinMissionResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json,
                    InterimBeIndemniteFinMissionResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private InterimBeIndemniteFinMissionResponse toResponse(
            UUID caseFileId, InterimBeIndemniteFinMissionResult r) {
        return new InterimBeIndemniteFinMissionResponse(
                caseFileId,
                r.dateDebutMission(),
                r.dateFinPrevue(),
                r.dateFinReelle(),
                r.dureeReellePrestationJours(),
                r.dureePrevueJours(),
                r.salaireHoraireBrut(),
                r.heuresPrestees(),
                r.heuresSupplementairesSemaine(),
                r.heuresSupplementairesDimancheFerie(),
                r.peculeDejaVerseParFsi(),
                r.ruptureAnticipeeParEtiSansMotifGrave(),
                r.commissionParitaireUtilisateur(),
                r.ancienneteSectorielleJours(),
                r.verdict(),
                r.salaireBrutTotalPrestations(),
                r.peculeVacancesInterim(),
                r.primeFinAnneeSectorielle(),
                r.indemniteRuptureAnticipee(),
                r.sursalaireHeuresSupplementaires(),
                r.totalIndemnitesBrutes(),
                r.primePrecaritePresumee(),
                r.tauxPrimeFinAnneeApplique(),
                r.joursRestantsACourirJusquAuTerme(),
                r.ancienneteSectorielleSuffisante(),
                r.raison(),
                r.synthese(),
                r.baseJuridique(),
                r.avertissement());
    }
}
