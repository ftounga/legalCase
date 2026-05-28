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
 * SF-219-19 : service orchestrant l'analyse <i>droit à la déconnexion
 * BE</i> (Loi du 03/10/2022 « Deal pour l'emploi », art. 16 + AR
 * 19/02/2023 + CCT n° 149).
 *
 * <p>Gate <b>BELGIQUE strict</b> (404 côté FR — réponse indistinguable
 * d'un dossier inconnu, pattern miroir SF-219-06/08/10/12/14/15/16/18) +
 * isolation workspace standard + gate domaine
 * {@code DROIT_DU_TRAVAIL} (400) + persistance JSON snapshot.</p>
 *
 * <p>Le checker sous-jacent ({@link DroitDeconnexionBeChecker}) est une
 * fonction pure indépendante du contexte HTTP / persistance.</p>
 */
@Service
public class DroitDeconnexionBeService {

    private final DroitDeconnexionBeAnalysisRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public DroitDeconnexionBeService(
            DroitDeconnexionBeAnalysisRepository repository,
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
    public DroitDeconnexionBeResponse analyze(
            UUID caseFileId,
            DroitDeconnexionBeRequest request,
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
        // SF-219-06/08/10/12/14/15/16/18. La France a un droit à la
        // déconnexion distinct (C. trav. art. L. 2242-17, 7° ; Loi
        // n° 2016-1088 du 08/08/2016 dite « El Khomri » ; obligation
        // de négociation annuelle sur la qualité de vie et conditions
        // de travail dans les entreprises ≥ 50 salariés). Restitution
        // séparée par outil — pas de tentative d'harmonisation FR/BE.
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Case file not found");
        }

        DroitDeconnexionBeResult result;
        try {
            result = DroitDeconnexionBeChecker.analyze(request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    e.getMessage());
        }

        DroitDeconnexionBeResponse response = toResponse(caseFileId, result);

        DroitDeconnexionBeAnalysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseGet(() -> {
                            DroitDeconnexionBeAnalysis a =
                                    new DroitDeconnexionBeAnalysis();
                            a.setCaseFile(caseFile);
                            return a;
                        });
        entity.setResultData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public DroitDeconnexionBeResponse get(
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

        DroitDeconnexionBeAnalysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Aucune analyse droit à la déconnexion"
                                        + " BE trouvée pour ce dossier"));
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

    private DroitDeconnexionBeResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json,
                    DroitDeconnexionBeResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private DroitDeconnexionBeResponse toResponse(
            UUID caseFileId, DroitDeconnexionBeResult r) {
        return new DroitDeconnexionBeResponse(
                caseFileId,
                r.effectifEntreprise(),
                r.statutAccord(),
                r.dateEntreeVigueurInstrument(),
                r.modalitesPratiquesDeconnexionDefinies(),
                r.sensibilisationFormationPrevue(),
                r.modalitesOrganisationTravailDefinies(),
                r.consultationOrganeConcertationEffectuee(),
                r.manquementSignaleCbeOuSpf(),
                r.verdict(),
                r.seuilAtteint(),
                r.instrumentFormalise(),
                r.contenuComplet(),
                r.modalitesPratiquesRespectees(),
                r.sensibilisationRespectee(),
                r.modalitesOrganisationRespectees(),
                r.consultationRespectee(),
                r.raison(),
                r.synthese(),
                r.baseJuridique(),
                r.avertissement());
    }
}
