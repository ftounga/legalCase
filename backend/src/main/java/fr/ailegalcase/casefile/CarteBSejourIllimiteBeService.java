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
 * SF-221-02 : service applicatif de l'outil carte B séjour illimité BE
 * (Loi 15/12/1980 art. 14 — passage carte A → séjour illimité après 5 ans).
 *
 * <p>Gates :
 * <ul>
 *   <li>workspace.country = BELGIQUE (sinon 400 — outil BE-only) ;</li>
 *   <li>caseFile.legalDomain = DROIT_IMMIGRATION (sinon 400) ;</li>
 *   <li>champ invalide (date absente / future, booléen manquant) → 400 ;</li>
 *   <li>isolation workspace par {@code WorkspaceMemberRepository} (sinon 404).</li>
 * </ul>
 */
@Service
public class CarteBSejourIllimiteBeService {

    private final CarteBSejourIllimiteBeRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public CarteBSejourIllimiteBeService(
            CarteBSejourIllimiteBeRepository repository,
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
    public CarteBSejourIllimiteBeResponse calculate(UUID caseFileId,
                                                    CarteBSejourIllimiteBeRequest request,
                                                    OidcUser oidcUser,
                                                    Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Carte B séjour illimité BE uniquement — pour la France voir les outils "
                            + "de carte de résident CESEDA.");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        CarteBSejourIllimiteBeResult result;
        try {
            result = CarteBSejourIllimiteBeCalculator.compute(
                    request.dateDebutSejourRegulier(),
                    request.sejourIninterrompu(),
                    request.absencesSuperieuresLimites(),
                    request.motifSejourStable(),
                    request.ordrePublicRisque());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        CarteBSejourIllimiteBeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    CarteBSejourIllimiteBeAnalysis a = new CarteBSejourIllimiteBeAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDateDebutSejourRegulier(result.dateDebutSejourRegulier());
        entity.setSejourIninterrompu(result.sejourIninterrompu());
        entity.setAbsencesSuperieuresLimites(result.absencesSuperieuresLimites());
        entity.setMotifSejourStable(result.motifSejourStable());
        entity.setOrdrePublicRisque(result.ordrePublicRisque());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, result);
    }

    @Transactional(readOnly = true)
    public CarteBSejourIllimiteBeResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        CarteBSejourIllimiteBeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse carte B séjour illimité BE trouvée pour ce dossier"));
        return toResponse(caseFileId, deserialize(entity.getResultData()));
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
        if (!"DROIT_IMMIGRATION".equals(cf.getLegalDomain())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ce dossier n'est pas un dossier de droit de l'immigration");
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

    private CarteBSejourIllimiteBeResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, CarteBSejourIllimiteBeResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private CarteBSejourIllimiteBeResponse toResponse(UUID caseFileId, CarteBSejourIllimiteBeResult r) {
        return new CarteBSejourIllimiteBeResponse(
                caseFileId,
                r.dateDebutSejourRegulier(),
                r.sejourIninterrompu(),
                r.absencesSuperieuresLimites(),
                r.motifSejourStable(),
                r.ordrePublicRisque(),
                r.verdict(),
                r.dureeSejourMois(),
                r.moisRestants(),
                r.basesJuridiques() != null ? r.basesJuridiques() : java.util.List.of(),
                r.messages() != null ? r.messages() : java.util.List.of());
    }
}
