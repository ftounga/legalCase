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
 * SF-216-17 : service orchestrant l'outil Adoption internationale FR
 * (art. 370-3 à 370-5 Cciv + Convention La Haye 1993). Gate FRANCE +
 * DROIT_FAMILLE.
 */
@Service
public class AdoptionInternationaleService {

    private final AdoptionInternationaleAnalysisRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public AdoptionInternationaleService(
            AdoptionInternationaleAnalysisRepository repository,
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
    public AdoptionInternationaleResponse calculate(UUID caseFileId,
                                                    AdoptionInternationaleRequest request,
                                                    OidcUser oidcUser,
                                                    Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;

        validateRequest(request, country);

        AdoptionInternationaleResult result;
        try {
            result = AdoptionInternationaleCalculator.compute(request, country);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        AdoptionInternationaleAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    AdoptionInternationaleAnalysis a = new AdoptionInternationaleAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public AdoptionInternationaleResponse get(UUID caseFileId,
                                              OidcUser oidcUser,
                                              Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        AdoptionInternationaleAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse Adoption internationale trouvée pour ce dossier"));
        String country = caseFile.getWorkspace() != null
                ? caseFile.getWorkspace().getCountry() : null;
        return toResponse(caseFileId, country, deserializeResult(entity.getResultData()));
    }

    // -----------------------------------------------------------------------
    // Privé
    // -----------------------------------------------------------------------

    private void validateRequest(AdoptionInternationaleRequest req, String country) {
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Outil F-FA-ADOPTION-INTERNATIONALE applicable uniquement en France (art. 370-3 Cciv).");
        }
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête manquant.");
        }
        if (req.paysOrigineEnfant() == null || req.paysOrigineEnfant().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "paysOrigineEnfant est requis.");
        }
        if (req.agrement2025() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "agrement2025 est requis (true / false).");
        }
        rejectNegativeAge(req.ageAdoptant(), "ageAdoptant");
        rejectNegativeAge(req.ageAdopte(), "ageAdopte");
    }

    private static void rejectNegativeAge(Integer v, String field) {
        if (v != null && v < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " doit être >= 0.");
        }
    }

    private User resolveUser(OidcUser oidcUser, Principal principal) {
        return currentUserResolver.resolve(oidcUser,
                OAuthProviderResolver.resolve(principal), principal);
    }

    private CaseFile resolveCaseFile(UUID caseFileId, User user) {
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        boolean member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(cf.getWorkspace().getId()))
                .orElse(false);
        if (!member) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        if (!"DROIT_FAMILLE".equals(cf.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit de la famille.");
        }
        return cf;
    }

    private String serialize(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation.");
        }
    }

    private AdoptionInternationaleResult deserializeResult(String json) {
        try {
            return objectMapper.readValue(json, AdoptionInternationaleResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation.");
        }
    }

    private AdoptionInternationaleResponse toResponse(UUID caseFileId, String country,
                                                      AdoptionInternationaleResult r) {
        return new AdoptionInternationaleResponse(
                caseFileId,
                r.conditionsRemplies(),
                r.voieProcedure(),
                r.conventionApplicable(),
                r.alerteKafala(),
                r.exequaturRequis(),
                r.delaiEstime(),
                r.verdict(),
                r.baseLegale(),
                r.messages(),
                r.alertes(),
                country
        );
    }
}
