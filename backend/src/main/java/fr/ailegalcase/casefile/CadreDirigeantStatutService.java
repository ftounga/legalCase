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
 * SF-218-19 : service applicatif de l'outil "Cadre dirigeant — qualification"
 * (F-DT-107) — évalue les 3 critères cumulatifs de l'art. L.3111-2 CT, détermine
 * la qualification (cadre dirigeant / fragile / non cadre dirigeant), l'exclusion
 * des règles de durée du travail et le risque de rappel d'heures
 * supplémentaires. Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <p>Gates :
 * <ul>
 *   <li>workspace.country = FRANCE (sinon 400 — outil FR-only) ;</li>
 *   <li>caseFile.legalDomain = DROIT_DU_TRAVAIL (sinon 400) ;</li>
 *   <li>3 critères booléens requis présents, rémunération ≥ 0 (sinon 400) ;</li>
 *   <li>isolation workspace par {@code WorkspaceMemberRepository} (sinon 404).</li>
 * </ul>
 */
@Service
public class CadreDirigeantStatutService {

    private final CadreDirigeantStatutRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public CadreDirigeantStatutService(CadreDirigeantStatutRepository repository,
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
    public CadreDirigeantStatutResponse analyze(UUID caseFileId,
                                                CadreDirigeantStatutRequest request,
                                                OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Cadre dirigeant — outil FRANCE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        CadreDirigeantStatutResult result;
        try {
            result = CadreDirigeantStatutAnalyzer.analyze(
                    request.independanceEmploiDuTemps(),
                    request.autonomieDecision(),
                    request.remunerationParmiPlusElevees(),
                    request.participationDirectionEntreprise(),
                    request.niveauRemunerationConstate());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        CadreDirigeantStatutAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    CadreDirigeantStatutAnalysis a = new CadreDirigeantStatutAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setIndependanceEmploiDuTemps(result.independanceEmploiDuTemps());
        entity.setAutonomieDecision(result.autonomieDecision());
        entity.setRemunerationParmiPlusElevees(result.remunerationParmiPlusElevees());
        entity.setParticipationDirectionEntreprise(result.participationDirectionEntreprise());
        entity.setNiveauRemunerationConstate(result.niveauRemunerationConstate());
        entity.setCriteresRemplis(result.criteresRemplis());
        entity.setQualification(result.qualification());
        entity.setExclusionDureeTravail(result.exclusionDureeTravail());
        entity.setRisqueRappelHeuresSupp(result.risqueRappelHeuresSupp());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public CadreDirigeantStatutResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        CadreDirigeantStatutAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse cadre dirigeant trouvée pour ce dossier"));
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

    private CadreDirigeantStatutResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, CadreDirigeantStatutResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private CadreDirigeantStatutResponse toResponse(UUID caseFileId, String country,
                                                    CadreDirigeantStatutResult r) {
        return new CadreDirigeantStatutResponse(
                caseFileId,
                r.independanceEmploiDuTemps(),
                r.autonomieDecision(),
                r.remunerationParmiPlusElevees(),
                r.participationDirectionEntreprise(),
                r.niveauRemunerationConstate(),
                r.criteres(),
                r.criteresRemplis(),
                r.qualification(),
                r.exclusionDureeTravail(),
                r.risqueRappelHeuresSupp(),
                country,
                r.baseJuridique());
    }
}
