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
 * SF-219-18 : service orchestrant l'analyse <i>semaine de 4 jours BE</i>
 * (Loi du 03/10/2022 « Deal pour l'emploi », art. 5).
 *
 * <p>Gate <b>BELGIQUE strict</b> (404 côté FR — réponse indistinguable
 * d'un dossier inconnu, pattern miroir SF-219-06/08/10/12/14/15/16) +
 * isolation workspace standard + gate domaine
 * {@code DROIT_DU_TRAVAIL} (400) + persistance JSON snapshot.</p>
 *
 * <p>Le checker sous-jacent ({@link Semaine4JoursBeChecker}) est une
 * fonction pure indépendante du contexte HTTP / persistance.</p>
 */
@Service
public class Semaine4JoursBeService {

    private final Semaine4JoursBeAnalysisRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public Semaine4JoursBeService(
            Semaine4JoursBeAnalysisRepository repository,
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
    public Semaine4JoursBeResponse analyze(
            UUID caseFileId,
            Semaine4JoursBeRequest request,
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
        // SF-219-06/08/10/12/14/15/16. Le régime français de la semaine
        // de 4 jours (Loi 13/06/1998 — réduction du temps de travail,
        // Loi 19/01/2000 dite « Aubry II », C. trav. art. L. 3122-2 et
        // suivants — accord d'entreprise organisant l'aménagement du
        // temps de travail) repose sur un mécanisme différent
        // (négociation collective majoritairement, et non demande
        // individuelle du salarié). Restitution séparée par outil.
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Case file not found");
        }

        Semaine4JoursBeResult result;
        try {
            result = Semaine4JoursBeChecker.analyze(request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    e.getMessage());
        }

        Semaine4JoursBeResponse response = toResponse(caseFileId, result);

        Semaine4JoursBeAnalysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseGet(() -> {
                            Semaine4JoursBeAnalysis a =
                                    new Semaine4JoursBeAnalysis();
                            a.setCaseFile(caseFile);
                            return a;
                        });
        entity.setResultData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public Semaine4JoursBeResponse get(
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

        Semaine4JoursBeAnalysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Aucune analyse semaine de 4 jours BE"
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

    private Semaine4JoursBeResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json,
                    Semaine4JoursBeResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private Semaine4JoursBeResponse toResponse(
            UUID caseFileId, Semaine4JoursBeResult r) {
        return new Semaine4JoursBeResponse(
                caseFileId,
                r.statutDemande(),
                r.travailleurTempsPlein(),
                r.demandeEcriteTravailleur(),
                r.dateDemande(),
                r.dureeHebdomadaireHeures(),
                r.journeeMaximaleHeures(),
                r.cctAutorise10h(),
                r.avenantEcritSigne(),
                r.reglementTravailModifie(),
                r.dureeAvenantMois(),
                r.avenantRenouvele(),
                r.refusMotiveParEcrit(),
                r.dateLicenciement(),
                r.motifLicenciementObjectifEtabli(),
                r.verdict(),
                r.eligibiliteRespectee(),
                r.demandeRespectee(),
                r.journeeRespectee(),
                r.formalisationRespectee(),
                r.dureeRespectee(),
                r.journeeMaximaleAutoriseeHeures(),
                r.raison(),
                r.synthese(),
                r.baseJuridique(),
                r.avertissement());
    }
}
