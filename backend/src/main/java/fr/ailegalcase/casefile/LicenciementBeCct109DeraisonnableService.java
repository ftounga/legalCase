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
 * SF-213-10 : service orchestrant le calcul du score CCT n° 109 (licenciement
 * manifestement déraisonnable, BELGIQUE — art. 9 CCT 12/02/2014).
 *
 * <p>Gate <b>BELGIQUE strict</b> (404 côté FR — réponse indistinguable d'un
 * dossier inconnu, pattern miroir SF-213-07/08) + isolation workspace
 * standard + persistance JSON snapshot (1:1 par dossier, upsert).</p>
 *
 * <p>Le calculateur sous-jacent
 * ({@link LicenciementBeCct109DeraisonnableCalculator}) est une fonction
 * pure indépendante du contexte HTTP / persistance.</p>
 */
@Service
public class LicenciementBeCct109DeraisonnableService {

    private final LicenciementBeCct109DeraisonnableAnalysisRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public LicenciementBeCct109DeraisonnableService(
            LicenciementBeCct109DeraisonnableAnalysisRepository repository,
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
    public LicenciementBeCct109DeraisonnableResponse analyze(
            UUID caseFileId,
            LicenciementBeCct109DeraisonnableRequest request,
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
        // dossier inconnu.
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Case file not found");
        }

        LicenciementBeCct109DeraisonnableResult result;
        try {
            result = LicenciementBeCct109DeraisonnableCalculator.analyze(request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        LicenciementBeCct109DeraisonnableResponse response = toResponse(caseFileId, result);

        LicenciementBeCct109DeraisonnableAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    LicenciementBeCct109DeraisonnableAnalysis a =
                            new LicenciementBeCct109DeraisonnableAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setResultData(serialize(response));
        repository.save(entity);

        return response;
    }

    @Transactional(readOnly = true)
    public LicenciementBeCct109DeraisonnableResponse get(
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

        LicenciementBeCct109DeraisonnableAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse CCT 109 trouvée pour ce dossier"));
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

    private LicenciementBeCct109DeraisonnableResponse deserialize(String json) {
        try {
            return objectMapper.readValue(json,
                    LicenciementBeCct109DeraisonnableResponse.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur de désérialisation");
        }
    }

    private LicenciementBeCct109DeraisonnableResponse toResponse(
            UUID caseFileId, LicenciementBeCct109DeraisonnableResult r) {
        return new LicenciementBeCct109DeraisonnableResponse(
                caseFileId,
                r.motifCommunique(),
                r.motifLieAPersonne(),
                r.discriminationSuspectee(),
                r.represaillesSuspectees(),
                r.proceduresRespectees(),
                r.remunerationHebdomadaireBrute(),
                r.argumentsPatronal(),
                r.echelonCct109(),
                r.nombreSemaines(),
                r.indemniteCct109(),
                r.justificationEchelon(),
                r.cumulAvecIcp(),
                r.baseJuridique(),
                r.avertissement());
    }
}
