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
 * SF-219-03 : service orchestrant l'analyse RCC BE entreprise en
 * difficulté / restructuration (Loi 26/12/2013 + CCT n° 17 + AR 03/05/2007
 * + AR reconnaissance ministre — conditions âge réduit du plan / carrière
 * ≥ 10 ans / ancienneté secteur ≥ 5 ans + reconnaissance obligatoire).
 *
 * <p>Gate <b>BELGIQUE strict</b> (404 côté FR — réponse indistinguable d'un
 * dossier inconnu, pattern miroir SF-213-08 / SF-219-01 / SF-219-02) +
 * isolation workspace standard + persistance JSON snapshot.</p>
 *
 * <p>Le validateur sous-jacent ({@link RccBeEntrepriseDifficulteValidator})
 * est une fonction pure indépendante du contexte HTTP / persistance.</p>
 */
@Service
public class RccBeEntrepriseDifficulteService {

    private final RccBeEntrepriseDifficulteAnalysisRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public RccBeEntrepriseDifficulteService(
            RccBeEntrepriseDifficulteAnalysisRepository repository,
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
    public RccBeEntrepriseDifficulteResponse analyze(
            UUID caseFileId,
            RccBeEntrepriseDifficulteRequest request,
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
        // dossier inconnu (aucun équivalent FR du RCC entreprise difficulté).
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Case file not found");
        }

        RccBeEntrepriseDifficulteResult result;
        try {
            result = RccBeEntrepriseDifficulteValidator.analyze(request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        RccBeEntrepriseDifficulteResponse response = toResponse(caseFileId, result);

        RccBeEntrepriseDifficulteAnalysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseGet(() -> {
                            RccBeEntrepriseDifficulteAnalysis a =
                                    new RccBeEntrepriseDifficulteAnalysis();
                            a.setCaseFile(caseFile);
                            return a;
                        });
        entity.setResultData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public RccBeEntrepriseDifficulteResponse get(
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

        RccBeEntrepriseDifficulteAnalysis entity =
                repository.findByCaseFileId(caseFileId)
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Aucune analyse RCC BE entreprise en difficulté "
                                        + "trouvée pour ce dossier"));
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

    private RccBeEntrepriseDifficulteResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json,
                    RccBeEntrepriseDifficulteResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private RccBeEntrepriseDifficulteResponse toResponse(
            UUID caseFileId, RccBeEntrepriseDifficulteResult r) {
        return new RccBeEntrepriseDifficulteResponse(
                caseFileId,
                r.typeReconnaissance(),
                r.ageReduitPlan(),
                r.ageFinContrat(),
                r.anneesCarriereTotale(),
                r.anneesAncienneteSecteur(),
                r.dateFinContrat(),
                r.licenciementEffectif(),
                r.remunerationNetteMensuelleReference(),
                r.allocationChomageMensuelleEstimee(),
                r.verdict(),
                r.eligible(),
                r.conditionReconnaissanceRemplie(),
                r.conditionAgeRemplie(),
                r.conditionCarriereRemplie(),
                r.conditionAncienneteRemplie(),
                r.conditionLicenciementRemplie(),
                r.indemniteComplementaireMensuelle(),
                r.synthese(),
                r.baseJuridique(),
                r.avertissement());
    }
}
