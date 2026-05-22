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
 * SF-216-03 : service orchestrant le calcul de la pension alimentaire enfant FR
 * (art. 371-2 Cciv). Gate FRANCE + DROIT_FAMILLE.
 */
@Service
public class PensionAlimentaireEnfantFrService {

    private final PensionAlimentaireEnfantFrRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public PensionAlimentaireEnfantFrService(
            PensionAlimentaireEnfantFrRepository repository,
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
    public PensionAlimentaireEnfantFrResponse calculate(UUID caseFileId,
                                                        PensionAlimentaireEnfantFrRequest request,
                                                        OidcUser oidcUser,
                                                        Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;

        validateRequest(request, country);

        PensionAlimentaireEnfantFrResult result;
        try {
            result = PensionAlimentaireEnfantFrCalculator.compute(request, country);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        PensionAlimentaireEnfantFrAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    PensionAlimentaireEnfantFrAnalysis a = new PensionAlimentaireEnfantFrAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public PensionAlimentaireEnfantFrResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        PensionAlimentaireEnfantFrAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de pension alimentaire enfant trouvée pour ce dossier"));
        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        return toResponse(caseFileId, country, deserializeResult(entity.getResultData()));
    }

    // -----------------------------------------------------------------------
    // Privé
    // -----------------------------------------------------------------------

    private void validateRequest(PensionAlimentaireEnfantFrRequest req, String country) {
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Outil F-FA-02 applicable uniquement en France (art. 371-2 Cciv).");
        }
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête manquant.");
        }
        rejectNegative(req.revenusNetsParent1Eur(), "revenusNetsParent1Eur");
        rejectNegative(req.revenusNetsParent2Eur(), "revenusNetsParent2Eur");
        if (req.nombreEnfants() == null || req.nombreEnfants() < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "nombreEnfants doit être ≥ 1.");
        }
        if (req.agesEnfants() == null || req.agesEnfants().size() != req.nombreEnfants()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "agesEnfants doit avoir une longueur égale à nombreEnfants.");
        }
        if (req.modeResidence() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "modeResidence est requis.");
        }
    }

    private static void rejectNegative(Integer v, String field) {
        if (v != null && v < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, field + " doit être ≥ 0.");
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

    private PensionAlimentaireEnfantFrResult deserializeResult(String json) {
        try {
            return objectMapper.readValue(json, PensionAlimentaireEnfantFrResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation.");
        }
    }

    private PensionAlimentaireEnfantFrResponse toResponse(UUID caseFileId, String country,
                                                          PensionAlimentaireEnfantFrResult r) {
        return new PensionAlimentaireEnfantFrResponse(
                caseFileId,
                r.montantParEnfantMensuelEur(),
                r.totalMensuelEur(),
                r.tauxApplique(),
                r.coefficientResidence(),
                r.parentDebiteur(),
                r.baseJuridique(),
                r.messages(),
                r.alertes(),
                country
        );
    }
}
