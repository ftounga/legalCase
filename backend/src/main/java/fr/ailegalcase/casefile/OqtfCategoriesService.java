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
 * SF-214-09 : service de l'analyse de la catégorie d'OQTF L. 611-1 CESEDA
 * (1° à 7°) et des moyens de défense spécifiques. Outil single-country FR.
 */
@Service
public class OqtfCategoriesService {

    private final OqtfCategoriesRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public OqtfCategoriesService(OqtfCategoriesRepository repository,
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
    public OqtfCategoriesResponse analyze(UUID caseFileId, OqtfCategoriesRequest request,
                                          OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "OQTF catégories L.611-1 — outil FRANCE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.categorieL611() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "categorieL611 est requise (CAT_1 à CAT_7)");
        }
        if (request.dateNotificationOqtf() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "dateNotificationOqtf est requise");
        }

        OqtfCategoriesResult result;
        try {
            result = OqtfCategoriesAnalyzer.analyze(
                    request.categorieL611(),
                    request.dateNotificationOqtf(),
                    request.motifOqtf());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        OqtfCategoriesAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    OqtfCategoriesAnalysis a = new OqtfCategoriesAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setCategorieL611(result.categorieL611().name());
        entity.setDateNotificationOqtf(result.dateNotificationOqtf());
        entity.setMotifOqtf(result.motifOqtf());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public OqtfCategoriesResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        OqtfCategoriesAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse OQTF catégories trouvée pour ce dossier"));
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
        if (!member) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        if (!"DROIT_IMMIGRATION".equals(cf.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit de l'immigration");
        }
        return cf;
    }

    private String serialize(Object o) {
        try { return objectMapper.writeValueAsString(o); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation");
        }
    }

    private OqtfCategoriesResult deserialize(String json) {
        try { return objectMapper.readValue(json, OqtfCategoriesResult.class); }
        catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private OqtfCategoriesResponse toResponse(UUID caseFileId, String country,
                                             OqtfCategoriesResult r) {
        return new OqtfCategoriesResponse(
                caseFileId,
                r.categorieL611(),
                r.categorieLibelle(),
                r.dateNotificationOqtf(),
                r.motifOqtf(),
                country,
                r.baseJuridique(),
                r.moyensDefense(),
                r.delaiRecours(),
                r.delaiRecoursJours(),
                r.delaiRecoursHeures(),
                r.procedureParallele());
    }
}
