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
 * SF-218-13 : service applicatif de l'outil "Particulier employeur / CESU" —
 * calcule le préavis conventionnel et l'indemnité de licenciement / de rupture
 * d'un salarié du particulier employeur (CESU) ou d'un assistant maternel, selon
 * la CCN applicable. Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <p>Gates :
 * <ul>
 *   <li>workspace.country = FRANCE (sinon 400 — outil FR-only) ;</li>
 *   <li>caseFile.legalDomain = DROIT_DU_TRAVAIL (sinon 400) ;</li>
 *   <li>dates présentes et cohérentes, salaire &gt; 0, RETRAIT_ENFANT réservé à
 *       l'assistant maternel (sinon 400) ;</li>
 *   <li>isolation workspace par {@code WorkspaceMemberRepository} (sinon 404).</li>
 * </ul>
 */
@Service
public class ParticulierEmployeurCesuService {

    private final ParticulierEmployeurCesuRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public ParticulierEmployeurCesuService(ParticulierEmployeurCesuRepository repository,
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
    public ParticulierEmployeurCesuResponse analyze(UUID caseFileId,
                                                    ParticulierEmployeurCesuRequest request,
                                                    OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Particulier employeur / CESU — outil FRANCE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        ParticulierEmployeurCesuResult result;
        try {
            result = ParticulierEmployeurCesuAnalyzer.analyze(
                    request.dateEntree(),
                    request.dateRupture(),
                    request.categorieEmploye(),
                    request.causeRupture(),
                    request.salaireMensuelMoyen());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        ParticulierEmployeurCesuAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    ParticulierEmployeurCesuAnalysis a = new ParticulierEmployeurCesuAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDateEntree(result.dateEntree());
        entity.setDateRupture(result.dateRupture());
        entity.setCategorieEmploye(result.categorieEmploye());
        entity.setCauseRupture(result.causeRupture());
        entity.setSalaireMensuelMoyen(result.salaireMensuelMoyen());
        entity.setDureePreavisJours(result.dureePreavisJours());
        entity.setIndemniteLicenciement(result.indemniteLicenciement());
        entity.setEligibiliteIndemnite(result.eligibiliteIndemnite());
        entity.setVerdict(result.verdict());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public ParticulierEmployeurCesuResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        ParticulierEmployeurCesuAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse particulier employeur / CESU trouvée pour ce dossier"));
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

    private ParticulierEmployeurCesuResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, ParticulierEmployeurCesuResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private ParticulierEmployeurCesuResponse toResponse(UUID caseFileId, String country,
                                                        ParticulierEmployeurCesuResult r) {
        return new ParticulierEmployeurCesuResponse(
                caseFileId,
                r.dateEntree(),
                r.dateRupture(),
                r.categorieEmploye(),
                r.causeRupture(),
                r.salaireMensuelMoyen(),
                r.ancienneteMois(),
                r.dureePreavisJours(),
                r.dureePreavisLibelle(),
                r.eligibiliteIndemnite(),
                r.motifNonDue(),
                r.indemniteLicenciement(),
                r.methodeCalcul(),
                r.verdict(),
                country,
                r.baseJuridique());
    }
}
