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
 * SF-218-53 : service applicatif de l'outil "droit à la déconnexion — conformité"
 * (art. L.2242-17 7° CT, F-DT-83) — apprécie la conformité à l'obligation de
 * négocier le droit à la déconnexion (entreprises d'au moins 50 salariés dotées
 * d'au moins un délégué syndical) et rend une checklist + un verdict. Outil
 * <b>FRANCE UNIQUEMENT</b>.
 *
 * <p>Gates :
 * <ul>
 *   <li>workspace.country = FRANCE (sinon 400 — outil FR-only) ;</li>
 *   <li>caseFile.legalDomain = DROIT_DU_TRAVAIL (sinon 400) ;</li>
 *   <li>champs requis présents, effectif &gt; 0 (sinon 400) ;</li>
 *   <li>isolation workspace par {@code WorkspaceMemberRepository} (sinon 404).</li>
 * </ul>
 */
@Service
public class DroitDeconnexionConformiteService {

    private final DroitDeconnexionConformiteRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public DroitDeconnexionConformiteService(DroitDeconnexionConformiteRepository repository,
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
    public DroitDeconnexionConformiteResponse analyze(UUID caseFileId,
                                                      DroitDeconnexionConformiteRequest request,
                                                      OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Droit à la déconnexion — conformité — outil FRANCE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        DroitDeconnexionConformiteResult result;
        try {
            result = DroitDeconnexionConformiteAnalyzer.analyze(
                    request.effectif(),
                    request.delegueSyndicalPresent(),
                    request.accordOuChartePresent(),
                    request.plagesDeconnexionDefinies(),
                    request.actionsSensibilisation(),
                    request.avisCseRecueilliPourCharte());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        DroitDeconnexionConformiteAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    DroitDeconnexionConformiteAnalysis a = new DroitDeconnexionConformiteAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setEffectif(result.effectif());
        entity.setDelegueSyndicalPresent(result.delegueSyndicalPresent());
        entity.setAccordOuChartePresent(result.accordOuChartePresent());
        entity.setPlagesDeconnexionDefinies(result.plagesDeconnexionDefinies());
        entity.setActionsSensibilisation(result.actionsSensibilisation());
        entity.setAvisCseRecueilliPourCharte(result.avisCseRecueilliPourCharte());
        entity.setObligationDeNegocier(result.obligationDeNegocier());
        entity.setItemsManquants(result.itemsManquants());
        entity.setStatut(result.statut());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public DroitDeconnexionConformiteResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        DroitDeconnexionConformiteAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de conformité au droit à la déconnexion trouvée pour ce dossier"));
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

    private DroitDeconnexionConformiteResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, DroitDeconnexionConformiteResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private DroitDeconnexionConformiteResponse toResponse(UUID caseFileId, String country,
                                                          DroitDeconnexionConformiteResult r) {
        return new DroitDeconnexionConformiteResponse(
                caseFileId,
                r.effectif(),
                r.delegueSyndicalPresent(),
                r.accordOuChartePresent(),
                r.plagesDeconnexionDefinies(),
                r.actionsSensibilisation(),
                r.avisCseRecueilliPourCharte(),
                r.obligationDeNegocier(),
                r.checklist(),
                r.itemsManquants(),
                r.statut(),
                r.notes(),
                country,
                r.baseJuridique());
    }
}
