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
 * SF-214-13 : service applicatif de l'outil "Renouvellement — délai de dépôt
 * 2 mois avant" (art. R. 433-1 CESEDA). Outil <b>FRANCE UNIQUEMENT</b>
 * (droit des étrangers).
 *
 * <p>Gates :
 * <ul>
 *   <li>workspace.country = FRANCE (sinon 400 — outil FR-only) ;</li>
 *   <li>caseFile.legalDomain = DROIT_IMMIGRATION (sinon 400) ;</li>
 *   <li>dateExpirationTitre requise (sinon 400) ;</li>
 *   <li>isolation workspace par {@code WorkspaceMemberRepository} (sinon 404).</li>
 * </ul>
 *
 * <p>Un dépôt tardif (au-delà de l'expiration) n'est pas une erreur : il est
 * accepté avec {@code alerteRetard=true} (200).
 */
@Service
public class RenouvellementDelaiService {

    private final RenouvellementDelaiRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public RenouvellementDelaiService(RenouvellementDelaiRepository repository,
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
    public RenouvellementDelaiResponse analyze(UUID caseFileId, RenouvellementDelaiRequest request,
                                               OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Renouvellement — délai de dépôt (R. 433-1) — outil FRANCE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        RenouvellementDelaiResult result;
        try {
            result = RenouvellementDelaiCalculator.compute(
                    request.dateExpirationTitre(),
                    request.dateDepotDossier(),
                    request.typeTitre());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        RenouvellementDelaiAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    RenouvellementDelaiAnalysis a = new RenouvellementDelaiAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDateExpirationTitre(result.dateExpirationTitre());
        entity.setDateDepotDossier(result.dateDepotDossier());
        entity.setTypeTitre(result.typeTitre());
        entity.setStatut(result.statut());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public RenouvellementDelaiResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        RenouvellementDelaiAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de délai de renouvellement trouvée pour ce dossier"));
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

    private RenouvellementDelaiResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, RenouvellementDelaiResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private RenouvellementDelaiResponse toResponse(UUID caseFileId, String country,
                                                   RenouvellementDelaiResult r) {
        return new RenouvellementDelaiResponse(
                caseFileId,
                r.dateExpirationTitre(),
                r.dateDepotDossier(),
                r.typeTitre(),
                r.dateOptimalDepot(),
                r.dateDepotImperatif(),
                r.joursRestantsAvantOptimal(),
                r.joursRestantsAvantImperatif(),
                r.statut(),
                r.risqueIrruption(),
                r.alerteRetard(),
                r.recommandation(),
                country,
                r.baseJuridique()
        );
    }
}
