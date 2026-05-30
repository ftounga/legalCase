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
 * SF-218-01 : service applicatif de l'outil "Appel CPH cour d'appel" — appel
 * d'un jugement du Conseil de prud'hommes devant la chambre sociale de la Cour
 * d'appel (art. 538 CPC ; R. 1461-1 CPC). Outil <b>FRANCE UNIQUEMENT</b>
 * (procédure d'appel social du droit français).
 *
 * <p>Gates :
 * <ul>
 *   <li>workspace.country = FRANCE (sinon 400 — outil FR-only) ;</li>
 *   <li>caseFile.legalDomain = DROIT_DU_TRAVAIL (sinon 400) ;</li>
 *   <li>dateNotificationJugement requise / non future, partieAppelante requise
 *       (sinon 400) ;</li>
 *   <li>isolation workspace par {@code WorkspaceMemberRepository} (sinon 404).</li>
 * </ul>
 */
@Service
public class AppelCphService {

    private final AppelCphRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public AppelCphService(AppelCphRepository repository,
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
    public AppelCphResponse analyze(UUID caseFileId, AppelCphRequest request,
                                    OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Appel CPH cour d'appel (art. 538 CPC) — outil FRANCE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.dateNotificationJugement() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "dateNotificationJugement est requise");
        }
        if (request.partieAppelante() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "partieAppelante est requise");
        }

        AppelCphResult result;
        try {
            result = AppelCphAnalyzer.analyze(
                    request.dateNotificationJugement(),
                    request.partieAppelante(),
                    request.modeNotification(),
                    request.representationConstituee(),
                    request.jugementEnDernierRessort());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        AppelCphAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    AppelCphAnalysis a = new AppelCphAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDateNotificationJugement(result.dateNotificationJugement());
        entity.setPartieAppelante(result.partieAppelante());
        entity.setModeNotification(result.modeNotification());
        entity.setRepresentationConstituee(result.representationConstituee());
        entity.setJugementEnDernierRessort(result.jugementEnDernierRessort());
        entity.setDateLimiteAppel(result.dateLimiteAppel());
        entity.setVerdict(result.verdict());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public AppelCphResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        AppelCphAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse d'appel CPH trouvée pour ce dossier"));
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

    private AppelCphResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, AppelCphResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private AppelCphResponse toResponse(UUID caseFileId, String country, AppelCphResult r) {
        return new AppelCphResponse(
                caseFileId,
                r.dateNotificationJugement(),
                r.partieAppelante(),
                r.modeNotification(),
                r.representationConstituee(),
                r.jugementEnDernierRessort(),
                r.dateLimiteAppel(),
                r.joursRestants(),
                r.verdict(),
                r.checklist(),
                r.renvoiPourvoiCassation(),
                country,
                r.baseJuridique());
    }
}
