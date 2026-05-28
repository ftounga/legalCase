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
 * SF-219-32 : service orchestrant l'analyse <i>Interruption de carrière
 * pour congé parental BE</i> (Loi du 22/01/1985 art. 99 à 107quater +
 * AR du 29/10/1997 + CCT n° 64).
 *
 * <p>Gate <b>BELGIQUE strict</b> (404 côté FR — réponse indistinguable
 * d'un dossier inconnu, pattern miroir SF-219-06/.../-30) + isolation
 * workspace standard + gate domaine {@code DROIT_DU_TRAVAIL} (400) +
 * persistance JSON snapshot.</p>
 *
 * <p>La France a un régime distinct (Code du travail FR art. L. 1225-47
 * à L. 1225-60 — congé parental d'éducation à durée et base de calcul
 * AT différentes, allocations CAF distinctes ONEM). Restitution séparée
 * par outil — pas de tentative d'harmonisation FR/BE.</p>
 *
 * <p>Articulation avec les outils BE existants : F-DT-29
 * {@code credit-temps-be} couvre le crédit-temps CCT 103 (régime
 * universel sans motif spécifique) ; SF-219-32 (présente) couvre
 * spécifiquement le congé parental régi par la Loi 22/01/1985 et la
 * CCT 64 (droit individuel par enfant et par parent avec conditions
 * d'âge enfant et formes spécifiques).</p>
 *
 * <p>Le checker sous-jacent
 * ({@link InterruptionCarriereSoinsParentalChecker}) est une fonction
 * pure indépendante du contexte HTTP / persistance.</p>
 */
@Service
public class InterruptionCarriereSoinsParentalService {

    private final InterruptionCarriereSoinsParentalAnalysisRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public InterruptionCarriereSoinsParentalService(
            InterruptionCarriereSoinsParentalAnalysisRepository repository,
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
    public InterruptionCarriereSoinsParentalResponse analyze(
            UUID caseFileId,
            InterruptionCarriereSoinsParentalRequest request,
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
        // SF-219-06/.../-30. La France a un régime distinct (Code du
        // travail FR art. L. 1225-47 à L. 1225-60 congé parental
        // d'éducation, durée et base de calcul AT différentes, allocations
        // CAF distinctes ONEM). Restitution séparée par outil.
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Case file not found");
        }

        InterruptionCarriereSoinsParentalResult result;
        try {
            result = InterruptionCarriereSoinsParentalChecker.analyze(request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    e.getMessage());
        }

        InterruptionCarriereSoinsParentalResponse response =
                toResponse(caseFileId, result);

        InterruptionCarriereSoinsParentalAnalysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseGet(() -> {
                            InterruptionCarriereSoinsParentalAnalysis a =
                                    new InterruptionCarriereSoinsParentalAnalysis();
                            a.setCaseFile(caseFile);
                            return a;
                        });
        entity.setResultData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public InterruptionCarriereSoinsParentalResponse get(
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

        InterruptionCarriereSoinsParentalAnalysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Aucune analyse interruption carrière"
                                        + " congé parental trouvée pour"
                                        + " ce dossier"));
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

    private InterruptionCarriereSoinsParentalResponse deserialize(
            String json) {
        try {
            return objectMapper.readValue(json,
                    InterruptionCarriereSoinsParentalResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private InterruptionCarriereSoinsParentalResponse toResponse(
            UUID caseFileId,
            InterruptionCarriereSoinsParentalResult r) {
        return new InterruptionCarriereSoinsParentalResponse(
                caseFileId,
                r.forme(),
                r.ancienneteMois(),
                r.ageEnfantMois(),
                r.enfantHandicap(),
                r.soldeRestantMoisEtp(),
                r.modeNotification(),
                r.employeurAccepte(),
                r.employeurAdiffere(),
                r.cumulAllocationOnemDemande(),
                r.dateDebutInterruption(),
                r.dateNotification(),
                r.remunerationMensuelleBrute(),
                r.verdict(),
                r.ancienneteOk(),
                r.ageEnfantOk(),
                r.formalismeOk(),
                r.dureeInterruptionMois(),
                r.soldeRestantApresImputationMoisEtp(),
                r.allocationOnemMensuelle(),
                r.allocationOnemTotale(),
                r.dateFinInterruption(),
                r.finProtectionLicenciement(),
                r.protectionLicenciementActive(),
                r.indemniteProtectionEnCasLicenciement(),
                r.raison(),
                r.synthese(),
                r.baseJuridique(),
                r.avertissement());
    }
}
