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
 * SF-214-17 : service applicatif de l'outil "Demande OFPRA introduction (GUDA/ADA)"
 * (art. R. 521-1 et s. CESEDA). Outil <b>FRANCE UNIQUEMENT</b> (droit d'asile).
 *
 * <p>Gates :
 * <ul>
 *   <li>workspace.country = FRANCE (sinon 400 — outil FR-only) ;</li>
 *   <li>caseFile.legalDomain = DROIT_IMMIGRATION (sinon 400) ;</li>
 *   <li>isolation workspace par {@code WorkspaceMemberRepository} (sinon 404).</li>
 * </ul>
 */
@Service
public class OfpraIntroductionService {

    private final OfpraIntroductionRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public OfpraIntroductionService(OfpraIntroductionRepository repository,
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
    public OfpraIntroductionResponse analyze(UUID caseFileId, OfpraIntroductionRequest request,
                                             OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Introduction OFPRA (R. 521-1 et s.) — outil FRANCE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        OfpraIntroductionResult result;
        try {
            result = OfpraIntroductionCalculator.compute(
                    request.dateArriveeEnFrance(),
                    request.passageGudaEffectue(),
                    request.datePassageGuda(),
                    request.adaRequise(),
                    request.paysOrigine());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        OfpraIntroductionAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    OfpraIntroductionAnalysis a = new OfpraIntroductionAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDateArriveeEnFrance(result.dateArriveeEnFrance());
        entity.setPassageGudaEffectue(result.passageGudaEffectue());
        entity.setDatePassageGuda(result.datePassageGuda());
        entity.setAdaRequise(result.adaRequise());
        entity.setPaysOrigine(result.paysOrigine());
        entity.setStatutDelai(result.statutDelai());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public OfpraIntroductionResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        OfpraIntroductionAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse d'introduction OFPRA trouvée pour ce dossier"));
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
        if (!member) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }
        if (!"DROIT_IMMIGRATION".equals(cf.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit de l'immigration");
        }
        return cf;
    }

    private String serialize(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation");
        }
    }

    private OfpraIntroductionResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, OfpraIntroductionResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private OfpraIntroductionResponse toResponse(UUID caseFileId, String country,
                                                 OfpraIntroductionResult r) {
        return new OfpraIntroductionResponse(
                caseFileId,
                r.dateArriveeEnFrance(),
                r.passageGudaEffectue(),
                r.datePassageGuda(),
                r.adaRequise(),
                r.paysOrigine(),
                r.dateEcheanceIntroduction(),
                r.joursRestantsIntroduction(),
                r.statutDelai(),
                r.procedureAccelereeRisque(),
                r.etapesAPrendre(),
                r.piecesRequises(),
                r.recommandation(),
                country,
                r.baseJuridique()
        );
    }
}
