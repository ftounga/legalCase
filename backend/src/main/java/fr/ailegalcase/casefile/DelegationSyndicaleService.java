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
 * SF-218-33 : service applicatif de l'outil "Délégué syndical / RSS : désignation
 * et protection" (F-DT-69) — apprécie la régularité de la désignation (effectif,
 * représentativité, score personnel pour le DS) et le risque de nullité d'un
 * licenciement d'un salarié protégé (autorisation de l'inspecteur du travail).
 * Outil <b>FRANCE UNIQUEMENT</b>, distinct de F-DT-30 (statut protégé RP général)
 * et de F-DT-65 (élections CSE / représentativité).
 *
 * <p>Gates :
 * <ul>
 *   <li>workspace.country = FRANCE (sinon 400 — outil FR-only) ;</li>
 *   <li>caseFile.legalDomain = DROIT_DU_TRAVAIL (sinon 400) ;</li>
 *   <li>effectif &gt; 0, typeMandat présent, syndicatRepresentatif présent,
 *       score ∈ [0, 100] (sinon 400) ;</li>
 *   <li>isolation workspace par {@code WorkspaceMemberRepository} (sinon 404).</li>
 * </ul>
 */
@Service
public class DelegationSyndicaleService {

    private final DelegationSyndicaleRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public DelegationSyndicaleService(DelegationSyndicaleRepository repository,
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
    public DelegationSyndicaleResponse analyze(UUID caseFileId,
                                               DelegationSyndicaleRequest request,
                                               OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Délégué syndical / RSS — outil FRANCE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        DelegationSyndicaleResult result;
        try {
            result = DelegationSyndicaleAnalyzer.analyze(
                    request.effectif(),
                    request.typeMandat(),
                    request.syndicatRepresentatif(),
                    request.pourcentageScorePersonnel(),
                    request.dateDesignation(),
                    request.licenciementEnvisage(),
                    request.autorisationInspecteurTravail());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        DelegationSyndicaleAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    DelegationSyndicaleAnalysis a = new DelegationSyndicaleAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setEffectif(result.effectif());
        entity.setTypeMandat(result.typeMandat());
        entity.setSyndicatRepresentatif(result.syndicatRepresentatif());
        entity.setPourcentageScorePersonnel(result.pourcentageScorePersonnel());
        entity.setDateDesignation(result.dateDesignation());
        entity.setStatutDesignation(result.statutDesignation());
        entity.setStatutProtege(result.statutProtege());
        entity.setLicenciementEnvisage(result.licenciementEnvisage());
        entity.setAutorisationInspecteurTravail(result.autorisationInspecteurTravail());
        entity.setRisqueNulliteLicenciement(result.risqueNulliteLicenciement());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public DelegationSyndicaleResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        DelegationSyndicaleAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de délégation syndicale trouvée pour ce dossier"));
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

    private DelegationSyndicaleResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, DelegationSyndicaleResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private DelegationSyndicaleResponse toResponse(UUID caseFileId, String country,
                                                   DelegationSyndicaleResult r) {
        return new DelegationSyndicaleResponse(
                caseFileId,
                r.effectif(),
                r.typeMandat(),
                r.syndicatRepresentatif(),
                r.pourcentageScorePersonnel(),
                r.dateDesignation(),
                r.checklist(),
                r.statutDesignation(),
                r.statutProtege(),
                r.licenciementEnvisage(),
                r.autorisationInspecteurTravail(),
                r.risqueNulliteLicenciement(),
                r.consequences(),
                country,
                r.baseJuridique());
    }
}
