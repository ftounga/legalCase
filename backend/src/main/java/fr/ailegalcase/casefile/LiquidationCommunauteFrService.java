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
 * SF-216-05 : service orchestrant le calcul de la liquidation de communauté
 * légale (art. 1467-1517 Cciv). Gate FRANCE + DROIT_FAMILLE + COMMUNAUTE_LEGALE.
 */
@Service
public class LiquidationCommunauteFrService {

    private final LiquidationCommunauteFrRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public LiquidationCommunauteFrService(
            LiquidationCommunauteFrRepository repository,
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
    public LiquidationCommunauteFrResponse calculate(UUID caseFileId,
                                                     LiquidationCommunauteFrRequest request,
                                                     OidcUser oidcUser,
                                                     Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;

        validateRequest(request, country);

        LiquidationCommunauteFrResult result;
        try {
            result = LiquidationCommunauteFrCalculator.compute(request, country);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        LiquidationCommunauteFrAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    LiquidationCommunauteFrAnalysis a = new LiquidationCommunauteFrAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public LiquidationCommunauteFrResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        LiquidationCommunauteFrAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse de liquidation de communauté trouvée pour ce dossier"));
        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        return toResponse(caseFileId, country, deserializeResult(entity.getResultData()));
    }

    // -----------------------------------------------------------------------
    // Privé
    // -----------------------------------------------------------------------

    private void validateRequest(LiquidationCommunauteFrRequest req, String country) {
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Outil F-FA-04 applicable uniquement en France (art. 1467 Cciv).");
        }
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête manquant.");
        }
        // Tous les montants ≥ 0 si renseignés
        rejectNegative(req.valeurImmeubleCommun1Eur(), "valeurImmeubleCommun1Eur");
        rejectNegative(req.valeurImmeubleCommun2Eur(), "valeurImmeubleCommun2Eur");
        rejectNegative(req.capitalRestantDuEur(), "capitalRestantDuEur");
        rejectNegative(req.valeurMobilierCommunEur(), "valeurMobilierCommunEur");
        rejectNegative(req.autresActifsCommunsEur(), "autresActifsCommunsEur");
        rejectNegative(req.recompensesEpoux1Eur(), "recompensesEpoux1Eur");
        rejectNegative(req.recompensesEpoux2Eur(), "recompensesEpoux2Eur");
        rejectNegative(req.biensPropresEpoux1Eur(), "biensPropresEpoux1Eur");
        rejectNegative(req.biensPropresEpoux2Eur(), "biensPropresEpoux2Eur");
        // Régime : si renseigné, doit être COMMUNAUTE_LEGALE
        if (req.regimeMatrimonialDetecte() != null
                && !LiquidationCommunauteFrCalculator.REGIME_REQUIS.equals(req.regimeMatrimonialDetecte())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Outil applicable uniquement en communauté légale. Régime fourni : "
                            + req.regimeMatrimonialDetecte() + ".");
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

    private LiquidationCommunauteFrResult deserializeResult(String json) {
        try {
            return objectMapper.readValue(json, LiquidationCommunauteFrResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation.");
        }
    }

    private LiquidationCommunauteFrResponse toResponse(UUID caseFileId, String country,
                                                       LiquidationCommunauteFrResult r) {
        return new LiquidationCommunauteFrResponse(
                caseFileId,
                r.masseCommuneEur(),
                r.quotaPartEpoux1Eur(),
                r.quotaPartEpoux2Eur(),
                r.soulteEur(),
                r.recompensesNettesEpoux1Eur(),
                r.recompensesNettesEpoux2Eur(),
                r.alerteIndivision(),
                r.baseJuridique(),
                r.messages(),
                r.alertes(),
                country
        );
    }
}
