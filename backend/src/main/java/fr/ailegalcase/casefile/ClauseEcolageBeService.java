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
 * SF-219-17 : service orchestrant l'analyse <i>clause d'écolage BE</i>
 * (art. 22bis Loi 03/07/1978).
 *
 * <p>Gate <b>BELGIQUE strict</b> (404 côté FR — réponse indistinguable
 * d'un dossier inconnu, pattern miroir SF-213-01 /
 * SF-219-06/08/10/12/14/16) + isolation workspace standard + gate
 * domaine {@code DROIT_DU_TRAVAIL} (400) + persistance JSON
 * snapshot.</p>
 *
 * <p>Le validateur sous-jacent ({@link ClauseEcolageBeValidator}) est
 * une fonction pure indépendante du contexte HTTP / persistance /
 * horloge.</p>
 */
@Service
public class ClauseEcolageBeService {

    private final ClauseEcolageBeAnalysisRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public ClauseEcolageBeService(
            ClauseEcolageBeAnalysisRepository repository,
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
    public ClauseEcolageBeResponse analyze(
            UUID caseFileId,
            ClauseEcolageBeRequest request,
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
        // l'existence de l'outil côté FR. En droit français, la clause
        // de dédit-formation (jurisprudence Cass. soc. 17/07/1991 — Le
        // Berre) suit un régime de validité substantiel différent
        // (cause réelle et sérieuse, durée raisonnable, prorata temporis
        // libre) et n'a pas de plafond légal 80 % ni de dégressivité
        // par tiers comme l'art. 22bis BE. Restitution séparée par
        // outil.
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Case file not found");
        }

        ClauseEcolageBeResult result;
        try {
            result = ClauseEcolageBeValidator.validate(request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    e.getMessage());
        }

        ClauseEcolageBeResponse response = toResponse(caseFileId, result);

        ClauseEcolageBeAnalysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseGet(() -> {
                            ClauseEcolageBeAnalysis a =
                                    new ClauseEcolageBeAnalysis();
                            a.setCaseFile(caseFile);
                            return a;
                        });
        entity.setResultData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public ClauseEcolageBeResponse get(
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

        ClauseEcolageBeAnalysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Aucune analyse clause d'écolage BE"
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

    private ClauseEcolageBeResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json,
                    ClauseEcolageBeResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private ClauseEcolageBeResponse toResponse(
            UUID caseFileId, ClauseEcolageBeResult r) {
        return new ClauseEcolageBeResponse(
                caseFileId,
                r.typeFormation(),
                r.clauseEcriteAvantEntreeFormation(),
                r.coutReelFormationEuros(),
                r.rmmmgMensuelEuros(),
                r.dureeEfficaciteMois(),
                r.dateFinFormation(),
                r.dateDepartTravailleur(),
                r.motifDepart(),
                r.verdict(),
                r.coutMinLegalEuros(),
                r.moisEcoulesDepuisFinFormation(),
                r.tierDureeAuDepart(),
                r.quotiteDueRatio(),
                r.montantBrutDueEuros(),
                r.plafond80Euros(),
                r.montantDuFinalEuros(),
                r.raison(),
                r.synthese(),
                r.baseJuridique(),
                r.avertissement());
    }
}
