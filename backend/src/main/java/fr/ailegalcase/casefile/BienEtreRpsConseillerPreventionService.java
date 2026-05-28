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
 * SF-219-30 : service orchestrant l'analyse <i>Saisine du Conseiller
 * en Prévention Aspects Psychosociaux (CPAP) — procédure RPS interne
 * BE</i> (Loi du 04/08/1996 art. 32sexies + AR du 10/04/2014).
 *
 * <p>Gate <b>BELGIQUE strict</b> (404 côté FR — réponse indistinguable
 * d'un dossier inconnu, pattern miroir SF-219-06/.../-29) + isolation
 * workspace standard + gate domaine {@code DROIT_DU_TRAVAIL} (400) +
 * persistance JSON snapshot.</p>
 *
 * <p>La France a un régime distinct (Code du travail FR art. L. 4121-1
 * et suivants — obligation employeur de sécurité, document unique
 * d'évaluation des risques DUERP art. R. 4121-1, procédure de
 * signalement spécifique aux référents harcèlement art. L. 1153-5-1).
 * Restitution séparée par outil — pas de tentative d'harmonisation
 * FR/BE.</p>
 *
 * <p>Articulation avec les outils harcèlement BE existants :
 * SF-213-07 {@code harcelement-be-procedure-formelle} couvre la
 * checklist de la demande formelle déjà déposée et la protection
 * 12 mois ; F-DT-30 {@code harcelement-licenciement-nul-section}
 * couvre la nullité du licenciement représailles ; SF-219-30
 * (présente) couvre la <b>conformité procédurale de la saisine</b>
 * en amont — étape distincte non recouvrante.</p>
 *
 * <p>Le checker sous-jacent
 * ({@link BienEtreRpsConseillerPreventionChecker}) est une fonction
 * pure indépendante du contexte HTTP / persistance.</p>
 */
@Service
public class BienEtreRpsConseillerPreventionService {

    private final BienEtreRpsConseillerPreventionAnalysisRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public BienEtreRpsConseillerPreventionService(
            BienEtreRpsConseillerPreventionAnalysisRepository repository,
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
    public BienEtreRpsConseillerPreventionResponse analyze(
            UUID caseFileId,
            BienEtreRpsConseillerPreventionRequest request,
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
        // SF-219-06/.../-29. La France a un régime distinct (Code du
        // travail FR art. L. 4121-1 et suivants, référent harcèlement
        // art. L. 1153-5-1, document unique R. 4121-1). Restitution
        // séparée par outil.
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Case file not found");
        }

        BienEtreRpsConseillerPreventionResult result;
        try {
            result = BienEtreRpsConseillerPreventionChecker.analyze(request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    e.getMessage());
        }

        BienEtreRpsConseillerPreventionResponse response =
                toResponse(caseFileId, result);

        BienEtreRpsConseillerPreventionAnalysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseGet(() -> {
                            BienEtreRpsConseillerPreventionAnalysis a =
                                    new BienEtreRpsConseillerPreventionAnalysis();
                            a.setCaseFile(caseFile);
                            return a;
                        });
        entity.setResultData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public BienEtreRpsConseillerPreventionResponse get(
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

        BienEtreRpsConseillerPreventionAnalysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Aucune analyse saisine CPAP RPS trouvée"
                                        + " pour ce dossier"));
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

    private BienEtreRpsConseillerPreventionResponse deserialize(
            String json) {
        try {
            return objectMapper.readValue(json,
                    BienEtreRpsConseillerPreventionResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private BienEtreRpsConseillerPreventionResponse toResponse(
            UUID caseFileId,
            BienEtreRpsConseillerPreventionResult r) {
        return new BienEtreRpsConseillerPreventionResponse(
                caseFileId,
                r.typeRisque(),
                r.modeDemande(),
                r.etapeProcedure(),
                r.conseillerIdentifie(),
                r.entretienPrealableRealise(),
                r.demandeEcriteSignee(),
                r.accuseReceptionRecu(),
                r.notificationEmployeurFaite(),
                r.dateDepotDemande(),
                r.dateAvisRendu(),
                r.joursDepuisDepot(),
                r.verdict(),
                r.formalitesPrealablesCompletes(),
                r.delaiEnqueteRespecte(),
                r.echeanceEnqueteTroisMois(),
                r.echeanceMesuresEmployeurDeuxMois(),
                r.finProtectionRepresailles(),
                r.protectionRepresaillesActive(),
                r.raison(),
                r.synthese(),
                r.baseJuridique(),
                r.avertissement());
    }
}
