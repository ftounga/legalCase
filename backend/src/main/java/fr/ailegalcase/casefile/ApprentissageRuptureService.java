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
 * SF-218-23 : service applicatif de l'outil "Apprentissage — validité de la
 * rupture" (F-DT-110) — distingue la rupture libre des 45 premiers jours de la
 * rupture sur motif limité au-delà (accord écrit, faute grave, force majeure,
 * inaptitude, exclusion définitive du CFA) et qualifie la validité
 * (art. L.6222-18 et s. CT). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <p>Gates :
 * <ul>
 *   <li>workspace.country = FRANCE (sinon 400 — outil FR-only) ;</li>
 *   <li>caseFile.legalDomain = DROIT_DU_TRAVAIL (sinon 400) ;</li>
 *   <li>dates requises et cohérentes, auteur / motif requis (sinon 400) ;</li>
 *   <li>isolation workspace par {@code WorkspaceMemberRepository} (sinon 404).</li>
 * </ul>
 */
@Service
public class ApprentissageRuptureService {

    private final ApprentissageRuptureRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public ApprentissageRuptureService(ApprentissageRuptureRepository repository,
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
    public ApprentissageRuptureResponse analyze(UUID caseFileId,
                                                ApprentissageRuptureRequest request,
                                                OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Apprentissage — validité de la rupture — outil FRANCE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        ApprentissageRuptureResult result;
        try {
            result = ApprentissageRuptureAnalyzer.analyze(
                    request.dateDebutContrat(),
                    request.dateRupture(),
                    request.auteurRupture(),
                    request.motifRupture(),
                    request.apprentiMajeur());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        ApprentissageRuptureAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    ApprentissageRuptureAnalysis a = new ApprentissageRuptureAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDateDebutContrat(result.dateDebutContrat());
        entity.setDateRupture(result.dateRupture());
        entity.setAuteurRupture(result.auteurRupture());
        entity.setMotifRupture(result.motifRupture());
        entity.setJoursDepuisDebut(result.joursDepuisDebut());
        entity.setPeriode(result.periode());
        entity.setDansPeriodeLibre(result.dansPeriodeLibre());
        entity.setValidite(result.validite());
        entity.setVerdictGlobal(result.verdictGlobal());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public ApprentissageRuptureResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        ApprentissageRuptureAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de rupture d'apprentissage trouvée pour ce dossier"));
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

    private ApprentissageRuptureResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, ApprentissageRuptureResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private ApprentissageRuptureResponse toResponse(UUID caseFileId, String country,
                                                    ApprentissageRuptureResult r) {
        return new ApprentissageRuptureResponse(
                caseFileId,
                r.dateDebutContrat(),
                r.dateRupture(),
                r.auteurRupture(),
                r.motifRupture(),
                r.apprentiMajeur(),
                r.joursDepuisDebut(),
                r.periode(),
                r.dansPeriodeLibre(),
                r.validite(),
                r.verdictGlobal(),
                r.consequences(),
                r.motif(),
                country,
                r.baseJuridique());
    }
}
