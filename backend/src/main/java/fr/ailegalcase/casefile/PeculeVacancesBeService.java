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
 * SF-219-20 : service orchestrant le calcul du <i>pécule de vacances
 * BE</i> (Lois coordonnées du 28/06/1971 + AR du 30/03/1967).
 *
 * <p>Gate <b>BELGIQUE strict</b> (404 côté FR — réponse indistinguable
 * d'un dossier inconnu, pattern miroir SF-219-18) + isolation
 * workspace standard + gate domaine {@code DROIT_DU_TRAVAIL} (400)
 * + persistance JSON snapshot.</p>
 *
 * <p>Le calculateur sous-jacent ({@link PeculeVacancesBeCalculator})
 * est une fonction pure indépendante du contexte HTTP / persistance.</p>
 */
@Service
public class PeculeVacancesBeService {

    private final PeculeVacancesBeAnalysisRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public PeculeVacancesBeService(
            PeculeVacancesBeAnalysisRepository repository,
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
    public PeculeVacancesBeResponse analyze(
            UUID caseFileId,
            PeculeVacancesBeRequest request,
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
        // l'existence de l'outil côté FR — pattern miroir SF-219-18.
        // Aucun équivalent FR direct (la France a l'ICCP — indemnité
        // compensatrice de congés payés — calculée différemment :
        // 10 % de la rémunération brute totale ou maintien du salaire,
        // C. trav. art. L. 3141-24 et suivants ; pas de double pécule,
        // pas de débiteur tiers de type ONVA). Restitution séparée
        // par outil — pas de tentative d'harmonisation FR/BE.
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Case file not found");
        }

        PeculeVacancesBeResult result;
        try {
            result = PeculeVacancesBeCalculator.analyze(request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    e.getMessage());
        }

        PeculeVacancesBeResponse response = toResponse(caseFileId, result);

        PeculeVacancesBeAnalysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseGet(() -> {
                            PeculeVacancesBeAnalysis a =
                                    new PeculeVacancesBeAnalysis();
                            a.setCaseFile(caseFile);
                            return a;
                        });
        entity.setResultData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public PeculeVacancesBeResponse get(
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

        PeculeVacancesBeAnalysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Aucune analyse pécule de vacances BE"
                                        + " trouvée pour ce dossier"));
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

    private PeculeVacancesBeResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json,
                    PeculeVacancesBeResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private PeculeVacancesBeResponse toResponse(
            UUID caseFileId, PeculeVacancesBeResult r) {
        return new PeculeVacancesBeResponse(
                caseFileId,
                r.statut(),
                r.typeCalcul(),
                r.remunerationBruteAnnuelleExerciceEur(),
                r.remunerationMensuelleBruteEur(),
                r.joursCongesPris(),
                r.peculeDejaPaye(),
                r.remunerationBruteAnnuelleExercicePrecedentEur(),
                r.dateSortie(),
                r.dateFinContrat(),
                r.dateReclamation(),
                r.montantPeculeSimpleEur(),
                r.montantDoublePeculeEur(),
                r.montantPeculeDepartEur(),
                r.montantTotalDuEur(),
                r.fractionLegaleAppliquee(),
                r.prescrit(),
                r.joursDepuisFaitGenerateur(),
                r.verdict(),
                r.raison(),
                r.synthese(),
                r.baseJuridique(),
                r.avertissement());
    }
}
