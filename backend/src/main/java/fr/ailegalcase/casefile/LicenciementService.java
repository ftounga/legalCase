package fr.ailegalcase.casefile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.Map;
import java.util.UUID;

@Service
public class LicenciementService {

    private final LicenciementAnalysisRepository analysisRepository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public LicenciementService(LicenciementAnalysisRepository analysisRepository,
                                CaseFileRepository caseFileRepository,
                                WorkspaceMemberRepository workspaceMemberRepository,
                                CurrentUserResolver currentUserResolver,
                                ObjectMapper objectMapper) {
        this.analysisRepository = analysisRepository;
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public LicenciementResponse analyze(UUID caseFileId,
                                         LicenciementRequest request,
                                         OidcUser oidcUser,
                                         Principal principal) {
        if (!LicenciementCritereReferentiel.isCountryValid(request.country())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Pays non supporté : " + request.country());
        }
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFileForUser(caseFileId, user);

        LicenciementAnalysisResult result = LicenciementAnalyzer.analyze(
                request.country(), request.reponses() != null ? request.reponses() : Map.of());

        LicenciementAnalysis entity = analysisRepository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    LicenciementAnalysis a = new LicenciementAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });

        entity.setCountry(request.country());
        entity.setReponsesData(serialize(request.reponses() != null ? request.reponses() : Map.of()));
        entity.setResultData(serialize(result));
        analysisRepository.save(entity);

        return toResponse(caseFileId, result);
    }

    @Transactional(readOnly = true)
    public LicenciementResponse get(UUID caseFileId,
                                     OidcUser oidcUser,
                                     Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFileForUser(caseFileId, user);

        LicenciementAnalysis entity = analysisRepository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de licenciement trouvée pour ce dossier"));

        LicenciementAnalysisResult result = deserialize(entity.getResultData(), LicenciementAnalysisResult.class);
        return toResponse(caseFileId, result);
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        String provider = OAuthProviderResolver.resolve(principal);
        return currentUserResolver.resolve(oidcUser, provider, principal);
    }

    private CaseFile resolveCaseFileForUser(UUID caseFileId, User user) {
        CaseFile caseFile = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        boolean member = workspaceMemberRepository
                .findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(caseFile.getWorkspace().getId()))
                .orElse(false);
        if (!member) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        if (!"DROIT_DU_TRAVAIL".equals(caseFile.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit du travail");
        }
        return caseFile;
    }

    private String serialize(Object obj) {
        try { return objectMapper.writeValueAsString(obj); }
        catch (JsonProcessingException e) { throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation"); }
    }

    private <T> T deserialize(String json, Class<T> clazz) {
        try { return objectMapper.readValue(json, clazz); }
        catch (JsonProcessingException e) { throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation"); }
    }

    private LicenciementResponse toResponse(UUID caseFileId, LicenciementAnalysisResult result) {
        return new LicenciementResponse(
                caseFileId, result.country(), result.scoreRisque(), result.verdict(),
                result.criteres().stream().map(c -> new LicenciementResponse.CritereData(
                        c.code(), c.label(), c.reponse(), c.pointsRisque(), c.bloquant(), c.commentaire()
                )).toList()
        );
    }
}
