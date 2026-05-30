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
 * SF-218-09 : service applicatif de l'outil "Action de groupe en discrimination"
 * — recevabilité du contentieux collectif de la discrimination au travail
 * (art. L. 1134-7 à L. 1134-10 Code travail). Outil <b>FRANCE UNIQUEMENT</b>.
 *
 * <p>Gates :
 * <ul>
 *   <li>workspace.country = FRANCE (sinon 400 — outil FR-only) ;</li>
 *   <li>caseFile.legalDomain = DROIT_DU_TRAVAIL (sinon 400) ;</li>
 *   <li>typeOrganisation / motifDiscrimination requis, nombrePersonnesConcernees
 *       ≥ 1, dateMiseEnDemeure non future (sinon 400) ;</li>
 *   <li>isolation workspace par {@code WorkspaceMemberRepository} (sinon 404).</li>
 * </ul>
 */
@Service
public class ActionGroupeDiscriminationService {

    private final ActionGroupeDiscriminationRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public ActionGroupeDiscriminationService(ActionGroupeDiscriminationRepository repository,
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
    public ActionGroupeDiscriminationResponse analyze(UUID caseFileId,
                                                      ActionGroupeDiscriminationRequest request,
                                                      OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Action de groupe en discrimination (L. 1134-7 CT) — outil FRANCE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.typeOrganisation() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "typeOrganisation est requis");
        }
        if (request.motifDiscrimination() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "motifDiscrimination est requis");
        }
        if (request.nombrePersonnesConcernees() == null || request.nombrePersonnesConcernees() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "nombrePersonnesConcernees doit être supérieur ou égal à 1");
        }

        ActionGroupeDiscriminationResult result;
        try {
            result = ActionGroupeDiscriminationAnalyzer.analyze(
                    request.typeOrganisation(),
                    request.motifDiscrimination(),
                    request.nombrePersonnesConcernees(),
                    request.objetAction(),
                    request.dateMiseEnDemeure());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        ActionGroupeDiscriminationAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    ActionGroupeDiscriminationAnalysis a = new ActionGroupeDiscriminationAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setTypeOrganisation(result.typeOrganisation());
        entity.setMotifDiscrimination(result.motifDiscrimination());
        entity.setNombrePersonnesConcernees(result.nombrePersonnesConcernees());
        entity.setObjetAction(result.objetAction());
        entity.setDateMiseEnDemeure(result.dateMiseEnDemeure());
        entity.setQualiteAAgir(result.qualiteAAgir());
        entity.setPluraliteEtablie(result.pluraliteEtablie());
        entity.setDateRecevabiliteSaisine(result.dateRecevabiliteSaisine());
        entity.setDelaiCarenceRespecte(result.delaiCarenceRespecte());
        entity.setVerdict(result.verdict());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public ActionGroupeDiscriminationResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        ActionGroupeDiscriminationAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse d'action de groupe en discrimination trouvée pour ce dossier"));
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

    private ActionGroupeDiscriminationResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, ActionGroupeDiscriminationResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private ActionGroupeDiscriminationResponse toResponse(UUID caseFileId, String country,
                                                          ActionGroupeDiscriminationResult r) {
        return new ActionGroupeDiscriminationResponse(
                caseFileId,
                r.typeOrganisation(),
                r.motifDiscrimination(),
                r.nombrePersonnesConcernees(),
                r.objetAction(),
                r.dateMiseEnDemeure(),
                r.qualiteAAgir(),
                r.pluraliteEtablie(),
                r.dateRecevabiliteSaisine(),
                r.delaiCarenceRespecte(),
                r.verdict(),
                r.checklist(),
                country,
                r.baseJuridique());
    }
}
