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
 * SF-219-21 : service orchestrant l'analyse <i>éco-chèques +
 * chèques-repas BE</i> (CCT n°98 du CNT du 20/02/2009 +
 * Loi 25/04/2014 + AR 03/02/2010 art. 19bis AR 28/11/1969).
 *
 * <p>Gate <b>BELGIQUE strict</b> (404 côté FR — réponse indistinguable
 * d'un dossier inconnu, pattern miroir SF-219-20) + isolation
 * workspace standard + gate domaine {@code DROIT_DU_TRAVAIL} (400)
 * + persistance JSON snapshot.</p>
 *
 * <p>Le calculateur sous-jacent ({@link EcoChequesChequesRepasBeCalculator})
 * est une fonction pure indépendante du contexte HTTP / persistance.</p>
 */
@Service
public class EcoChequesChequesRepasBeService {

    private final EcoChequesChequesRepasBeAnalysisRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public EcoChequesChequesRepasBeService(
            EcoChequesChequesRepasBeAnalysisRepository repository,
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
    public EcoChequesChequesRepasBeResponse analyze(
            UUID caseFileId,
            EcoChequesChequesRepasBeRequest request,
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
        // l'existence de l'outil côté FR — pattern miroir SF-219-20.
        // Aucun équivalent FR direct (la France a les titres-restaurant
        // — CGI art. 81 19° ter — mais le régime est différent : plafond
        // d'exonération social 7,18 EUR/jour en 2024, contribution
        // employeur 50-60 %, pas de CCT 98 équivalent pour les
        // éco-chèques — restitution séparée par outil).
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Case file not found");
        }

        EcoChequesChequesRepasBeResult result;
        try {
            result = EcoChequesChequesRepasBeCalculator.analyze(request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    e.getMessage());
        }

        EcoChequesChequesRepasBeResponse response =
                toResponse(caseFileId, result);

        EcoChequesChequesRepasBeAnalysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseGet(() -> {
                            EcoChequesChequesRepasBeAnalysis a =
                                    new EcoChequesChequesRepasBeAnalysis();
                            a.setCaseFile(caseFile);
                            return a;
                        });
        entity.setResultData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public EcoChequesChequesRepasBeResponse get(
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

        EcoChequesChequesRepasBeAnalysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Aucune analyse éco-chèques /"
                                        + " chèques-repas BE trouvée"
                                        + " pour ce dossier"));
        return deserialize(entity.getResultData());
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        return currentUserResolver.resolve(oidcUser,
                OAuthProviderResolver.resolve(principal), principal);
    }

    private CaseFile resolveCaseFile(UUID caseFileId, User user) {
        CaseFile cf = caseFileRepository
                .findByIdAndDeletedAtIsNull(caseFileId)
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

    private EcoChequesChequesRepasBeResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json,
                    EcoChequesChequesRepasBeResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private EcoChequesChequesRepasBeResponse toResponse(
            UUID caseFileId, EcoChequesChequesRepasBeResult r) {
        return new EcoChequesChequesRepasBeResponse(
                caseFileId,
                r.typeAvantage(),
                r.montantAnnuelEur(),
                r.valeurFacialeUnitaireEur(),
                r.contributionTravailleurUnitaireEur(),
                r.joursEffectivementPrestes(),
                r.cctSectorielleOuEntrepriseExiste(),
                r.conventionIndividuelleEcrite(),
                r.paiementElectronique(),
                r.cumulFraisBouche(),
                r.substitutionRemuneration(),
                r.dateAttribution(),
                r.plafondLegalApplicableEur(),
                r.montantExonereEur(),
                r.montantRequalifieEnRemunerationEur(),
                r.cotisationsOnssEstimeesEur(),
                r.verdict(),
                r.raison(),
                r.synthese(),
                r.baseJuridique(),
                r.avertissement());
    }
}
