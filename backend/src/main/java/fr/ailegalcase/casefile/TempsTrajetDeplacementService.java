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
 * SF-218-51 : service applicatif de l'outil "Temps de trajet / déplacement
 * professionnel" (art. L.3121-4 CT ; CJUE C-266/14 « Tyco », F-DT-81) — qualifie
 * le temps de trajet (temps de travail effectif ou non) et détermine si une
 * contrepartie (repos / financière) est due. Outil <b>FRANCE UNIQUEMENT</b>,
 * distinct du remboursement de frais de déplacement et de l'astreinte
 * (invariant « un outil = une situation »).
 *
 * <p>Gates :
 * <ul>
 *   <li>workspace.country = FRANCE (sinon 400 — outil FR-only) ;</li>
 *   <li>caseFile.legalDomain = DROIT_DU_TRAVAIL (sinon 400) ;</li>
 *   <li>champs requis présents, typeTrajet connu, minutes &ge; 0 (sinon 400) ;</li>
 *   <li>isolation workspace par {@code WorkspaceMemberRepository} (sinon 404).</li>
 * </ul>
 */
@Service
public class TempsTrajetDeplacementService {

    private final TempsTrajetDeplacementRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public TempsTrajetDeplacementService(TempsTrajetDeplacementRepository repository,
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
    public TempsTrajetDeplacementResponse analyze(UUID caseFileId,
                                                  TempsTrajetDeplacementRequest request,
                                                  OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Temps de trajet / déplacement — outil FRANCE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        TempsTrajetDeplacementResult result;
        try {
            result = TempsTrajetDeplacementAnalyzer.analyze(
                    request.typeTrajet(),
                    request.tempsTrajetQuotidienMinutes(),
                    request.tempsTrajetNormalMinutes(),
                    request.contrepartiePrevueAccord());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        TempsTrajetDeplacementAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    TempsTrajetDeplacementAnalysis a = new TempsTrajetDeplacementAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setQualification(result.qualification());
        entity.setTypeTrajet(result.typeTrajet());
        entity.setTempsTrajetQuotidienMinutes(result.tempsTrajetQuotidienMinutes());
        entity.setTempsTrajetNormalMinutes(result.tempsTrajetNormalMinutes());
        entity.setContrepartiePrevueAccord(result.contrepartiePrevueAccord());
        entity.setContrepartieDue(result.contrepartieDue());
        entity.setDepassementMinutes(result.depassementMinutes());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public TempsTrajetDeplacementResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        TempsTrajetDeplacementAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de temps de trajet / déplacement trouvée pour ce dossier"));
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
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation");
        }
    }

    private TempsTrajetDeplacementResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, TempsTrajetDeplacementResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private TempsTrajetDeplacementResponse toResponse(UUID caseFileId, String country,
                                                      TempsTrajetDeplacementResult r) {
        return new TempsTrajetDeplacementResponse(
                caseFileId,
                r.qualification(),
                r.typeTrajet(),
                r.tempsTrajetQuotidienMinutes(),
                r.tempsTrajetNormalMinutes(),
                r.contrepartiePrevueAccord(),
                r.contrepartieDue(),
                r.depassementMinutes(),
                r.base(),
                r.notes(),
                country,
                r.baseJuridique());
    }
}
