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
 * SF-216-15 : service orchestrant l'outil Adoption intra-familiale FR
 * (art. 343-1 al. 2 + 345-1 + 360-362 Cciv). Gate FRANCE + DROIT_FAMILLE.
 */
@Service
public class AdoptionIntraFrService {

    private final AdoptionIntraFrRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public AdoptionIntraFrService(
            AdoptionIntraFrRepository repository,
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
    public AdoptionIntraFrResponse calculate(UUID caseFileId,
                                             AdoptionIntraFrRequest request,
                                             OidcUser oidcUser,
                                             Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;

        validateRequest(request, country);

        AdoptionIntraFrResult result;
        try {
            result = AdoptionIntraFrCalculator.compute(request, country);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        AdoptionIntraFrAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    AdoptionIntraFrAnalysis a = new AdoptionIntraFrAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public AdoptionIntraFrResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        AdoptionIntraFrAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse Adoption intra-familiale trouvée pour ce dossier"));
        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        return toResponse(caseFileId, country, deserializeResult(entity.getResultData()));
    }

    // -----------------------------------------------------------------------
    // Privé
    // -----------------------------------------------------------------------

    private void validateRequest(AdoptionIntraFrRequest req, String country) {
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Outil F-FA-ADOPTION-INTRA applicable uniquement en France (art. 345-1 Cciv).");
        }
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête manquant.");
        }
        if (req.formeAdoptionDemandee() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "formeAdoptionDemandee est requis (PLENIERE | SIMPLE).");
        }
        if (req.mariageOuPacsAdoptantParent() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "mariageOuPacsAdoptantParent est requis (true / false).");
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
        return currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);
    }

    private CaseFile resolveCaseFile(UUID caseFileId, User user) {
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        boolean member = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId().equals(cf.getWorkspace().getId())).orElse(false);
        if (!member) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        if (!"DROIT_FAMILLE".equals(cf.getLegalDomain()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit de la famille.");
        return cf;
    }

    private String serialize(Object o) {
        try {
            return objectMapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de sérialisation.");
        }
    }

    private AdoptionIntraFrResult deserializeResult(String json) {
        try {
            return objectMapper.readValue(json, AdoptionIntraFrResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation.");
        }
    }

    private AdoptionIntraFrResponse toResponse(UUID caseFileId, String country,
                                               AdoptionIntraFrResult r) {
        return new AdoptionIntraFrResponse(
                caseFileId,
                r.formesPossibles(),
                r.formeDemandee(),
                r.conditionsRemplies(),
                r.alerteIrreversibilite(),
                r.consentementEnfantRequis(),
                r.baseLegale(),
                r.messages(),
                r.alertes(),
                country
        );
    }
}
