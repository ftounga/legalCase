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
import java.time.LocalDate;
import java.util.UUID;

/**
 * SF-214-27 : service de l'analyse de la procédure d'évaluation d'âge MNA
 * (F-IM-38-mna-evaluation-age-fr, FRANCE uniquement). Gate hard
 * {@code workspace.country != FRANCE} et {@code legalDomain != DROIT_IMMIGRATION}.
 */
@Service
public class MnaEvaluationAgeService {

    private final MnaEvaluationAgeRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public MnaEvaluationAgeService(MnaEvaluationAgeRepository repository,
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
    public MnaEvaluationAgeResponse analyze(UUID caseFileId, MnaEvaluationAgeRequest request,
                                            OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Évaluation d'âge MNA — outil FRANCE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.dateNaissanceDeclaree() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "dateNaissanceDeclaree est requise");
        }

        MnaEvaluationAgeResult result;
        try {
            result = new MnaEvaluationAgeAnalyzer(LocalDate.now()).analyze(
                    request.dateNaissanceDeclaree(),
                    request.evaluationASERefusee(),
                    request.dateRefusASE(),
                    request.examenOsseuxOrdonne(),
                    request.resultatExamenOsseux());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        MnaEvaluationAgeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    MnaEvaluationAgeAnalysis a = new MnaEvaluationAgeAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDateNaissanceDeclaree(result.dateNaissanceDeclaree());
        entity.setEvaluationASERefusee(result.evaluationASERefusee());
        entity.setDateRefusASE(result.dateRefusASE());
        entity.setExamenOsseuxOrdonne(result.examenOsseuxOrdonne());
        entity.setStatut(result.statut().name());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public MnaEvaluationAgeResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        MnaEvaluationAgeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse d'évaluation d'âge MNA trouvée pour ce dossier"));
        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        return toResponse(caseFileId, country, deserialize(entity.getResultData()));
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        return currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }

    private CaseFile resolveCaseFile(UUID caseFileId, User user) {
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        boolean member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(cf.getWorkspace().getId())).orElse(false);
        if (!member) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        if (!"DROIT_IMMIGRATION".equals(cf.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit de l'immigration");
        }
        return cf;
    }

    private String serialize(Object o) {
        try { return objectMapper.writeValueAsString(o); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation");
        }
    }

    private MnaEvaluationAgeResult deserialize(String json) {
        try { return objectMapper.readValue(json, MnaEvaluationAgeResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private MnaEvaluationAgeResponse toResponse(UUID caseFileId, String country,
                                                MnaEvaluationAgeResult r) {
        return new MnaEvaluationAgeResponse(
                caseFileId,
                country,
                r.dateNaissanceDeclaree(),
                r.ageDeclare(),
                r.evaluationASERefusee(),
                r.dateRefusASE(),
                r.dateEcheanceSaisineJE(),
                r.examenOsseuxOrdonne(),
                r.resultatExamenOsseux(),
                r.statut(),
                r.procedureASE(),
                r.contestationExamenOsseux(),
                r.droitsAttaches(),
                r.baseJuridique());
    }
}
