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
 * SF-219-05 : service orchestrant l'analyse de conformité de l'outplacement
 * BE général au titre du régime "préavis ≥ 30 semaines" (Loi du 05/09/2001
 * art. 11 + AR du 21/10/2007).
 *
 * <p>Gate <b>BELGIQUE strict</b> (404 côté FR — réponse indistinguable d'un
 * dossier inconnu, pattern miroir SF-213-08 / SF-219-01 / SF-219-02 /
 * SF-219-03 / SF-219-04) + isolation workspace standard + persistance JSON
 * snapshot.</p>
 *
 * <p>Le checker sous-jacent ({@link OutplacementBeGeneral30semChecker})
 * est une fonction pure indépendante du contexte HTTP / persistance.</p>
 */
@Service
public class OutplacementBeGeneral30semService {

    private final OutplacementBeGeneral30semAnalysisRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public OutplacementBeGeneral30semService(
            OutplacementBeGeneral30semAnalysisRepository repository,
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
    public OutplacementBeGeneral30semResponse analyze(
            UUID caseFileId,
            OutplacementBeGeneral30semRequest request,
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
        // l'existence de l'outil côté FR — réponse indistinguable d'un
        // dossier inconnu (aucun équivalent FR de l'outplacement régime
        // général 30 semaines).
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Case file not found");
        }

        OutplacementBeGeneral30semResult result;
        try {
            result = OutplacementBeGeneral30semChecker.analyze(request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        OutplacementBeGeneral30semResponse response = toResponse(caseFileId, result);

        OutplacementBeGeneral30semAnalysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseGet(() -> {
                            OutplacementBeGeneral30semAnalysis a =
                                    new OutplacementBeGeneral30semAnalysis();
                            a.setCaseFile(caseFile);
                            return a;
                        });
        entity.setResultData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public OutplacementBeGeneral30semResponse get(
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

        OutplacementBeGeneral30semAnalysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Aucune analyse d'outplacement BE général 30 "
                                        + "semaines trouvée pour ce dossier"));
        return deserialize(entity.getResultData());
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        return currentUserResolver.resolve(oidcUser,
                OAuthProviderResolver.resolve(principal), principal);
    }

    private CaseFile resolveCaseFile(UUID caseFileId, User user) {
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Case file not found"));
        boolean member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(cf.getWorkspace().getId()))
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
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de sérialisation");
        }
    }

    private OutplacementBeGeneral30semResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json,
                    OutplacementBeGeneral30semResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private OutplacementBeGeneral30semResponse toResponse(
            UUID caseFileId, OutplacementBeGeneral30semResult r) {
        return new OutplacementBeGeneral30semResponse(
                caseFileId,
                r.licenciementEffectif(),
                r.motifGrave(),
                r.semainesPreavisOuIndemnite(),
                r.dateFinContrat(),
                r.offreNotifiee(),
                r.dateNotificationOffre(),
                r.heuresProgramme(),
                r.dureeProgrammeMois(),
                r.offreEcrite(),
                r.offreIndividuelle(),
                r.contenuMinimumRespecte(),
                r.verdict(),
                r.conforme(),
                r.conditionLicenciementRemplie(),
                r.conditionMotifNonGraveRemplie(),
                r.conditionPreavis30semRemplie(),
                r.conditionOffreDansLes15joursRemplie(),
                r.conditionDureeProgrammeRemplie(),
                r.conditionFormeOffreRemplie(),
                r.indemniteForfaitaireTravailleur(),
                r.synthese(),
                r.baseJuridique(),
                r.avertissement());
    }
}
