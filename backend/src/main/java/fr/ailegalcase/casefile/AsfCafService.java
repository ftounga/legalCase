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
 * SF-222-01 : service orchestrant le calcul ASF (allocation de soutien familial,
 * art. L. 523-1 CSS). Gate FRANCE + DROIT_FAMILLE. Upsert 1:1 par dossier.
 */
@Service
public class AsfCafService {

    private final AsfCafRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public AsfCafService(
            AsfCafRepository repository,
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
    public AsfCafResponse calculate(UUID caseFileId,
                                    AsfCafRequest request,
                                    OidcUser oidcUser,
                                    Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;

        validateRequest(request, country);

        AsfCafResult result;
        try {
            result = AsfCafCalculator.compute(request, country);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        AsfCafAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    AsfCafAnalysis a = new AsfCafAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public AsfCafResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        AsfCafAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse ASF trouvée pour ce dossier"));
        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        return toResponse(caseFileId, country, deserializeResult(entity.getResultData()));
    }

    // -----------------------------------------------------------------------
    // Privé
    // -----------------------------------------------------------------------

    private void validateRequest(AsfCafRequest req, String country) {
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Outil F-FA-ASF-CAF applicable uniquement en France (art. L. 523-1 CSS).");
        }
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête manquant.");
        }
        if (req.nbEnfantsACharge() == null || req.nbEnfantsACharge() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "nbEnfantsACharge doit être un entier strictement positif.");
        }
        if (req.montantPensionMensuel() != null && req.montantPensionMensuel() < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "montantPensionMensuel doit être ≥ 0.");
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

    private AsfCafResult deserializeResult(String json) {
        try {
            return objectMapper.readValue(json, AsfCafResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation.");
        }
    }

    private AsfCafResponse toResponse(UUID caseFileId, String country, AsfCafResult r) {
        return new AsfCafResponse(
                caseFileId,
                r.verdict().name(),
                r.montantMensuelEstime(),
                r.recouvrementApplicable(),
                r.basesJuridiques(),
                r.messages(),
                country
        );
    }
}
