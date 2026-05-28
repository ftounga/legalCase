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
 * SF-219-31 : service orchestrant l'analyse <i>Congé paternité /
 * naissance BE</i> (Loi du 03/07/1978 art. 30 § 2 + Loi du 12/08/2000
 * + Loi du 07/04/2023 Deal pour l'emploi).
 *
 * <p>Gate <b>BELGIQUE strict</b> (404 côté FR — réponse indistinguable
 * d'un dossier inconnu, pattern miroir SF-219-06/.../-30) + isolation
 * workspace standard + gate domaine {@code DROIT_DU_TRAVAIL} (400) +
 * persistance JSON snapshot.</p>
 *
 * <p>La France a un régime distinct (Code du travail FR art. L. 1225-35
 * congé de paternité et d'accueil de l'enfant — 25 jours calendaires
 * dont 7 jours obligatoires depuis la Loi de financement de la sécurité
 * sociale 2021 art. 73). Restitution séparée par outil — pas de
 * tentative d'harmonisation FR/BE.</p>
 *
 * <p>Le checker sous-jacent
 * ({@link CongePaterniteNaissanceBeChecker}) est une fonction pure
 * indépendante du contexte HTTP / persistance.</p>
 */
@Service
public class CongePaterniteNaissanceBeService {

    private final CongePaterniteNaissanceBeAnalysisRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public CongePaterniteNaissanceBeService(
            CongePaterniteNaissanceBeAnalysisRepository repository,
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
    public CongePaterniteNaissanceBeResponse analyze(
            UUID caseFileId,
            CongePaterniteNaissanceBeRequest request,
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
        // travail FR art. L. 1225-35 congé de paternité et d'accueil de
        // l'enfant — 25 jours calendaires dont 7 obligatoires).
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Case file not found");
        }

        CongePaterniteNaissanceBeResult result;
        try {
            result = CongePaterniteNaissanceBeChecker.analyze(request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    e.getMessage());
        }

        CongePaterniteNaissanceBeResponse response =
                toResponse(caseFileId, result);

        CongePaterniteNaissanceBeAnalysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseGet(() -> {
                            CongePaterniteNaissanceBeAnalysis a =
                                    new CongePaterniteNaissanceBeAnalysis();
                            a.setCaseFile(caseFile);
                            return a;
                        });
        entity.setResultData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public CongePaterniteNaissanceBeResponse get(
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

        CongePaterniteNaissanceBeAnalysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Aucune analyse congé paternité / naissance"
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

    private CongePaterniteNaissanceBeResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json,
                    CongePaterniteNaissanceBeResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private CongePaterniteNaissanceBeResponse toResponse(
            UUID caseFileId,
            CongePaterniteNaissanceBeResult r) {
        return new CongePaterniteNaissanceBeResponse(
                caseFileId,
                r.statutTravailleur(),
                r.lienFiliation(),
                r.etapeProcedure(),
                r.filiationEtablie(),
                r.contratTravailEnCours(),
                r.notificationEmployeurFaite(),
                r.dateNaissance(),
                r.dateNotificationEmployeur(),
                r.joursDejaPrisOuvrables(),
                r.dateFinPriseEffective(),
                r.verdict(),
                r.dureeApplicableJoursOuvrables(),
                r.joursRestantsAPrendre(),
                r.echeanceQuatreMoisPostNaissance(),
                r.finProtectionLicenciement(),
                r.protectionLicenciementActive(),
                r.indemniteEmployeurJoursCent(),
                r.indemniteMutuelleJoursQuatreVingtDeux(),
                r.raison(),
                r.synthese(),
                r.baseJuridique(),
                r.avertissement());
    }
}
