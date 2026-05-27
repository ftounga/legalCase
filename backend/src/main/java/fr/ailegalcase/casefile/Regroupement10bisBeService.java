package fr.ailegalcase.casefile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.shared.OAuthProviderResolver;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.UUID;

/**
 * SF-215-05 : service applicatif de l'outil regroupement familial 10bis BE
 * (Loi 15/12/1980 art. 10 et 10bis + AR 17/05/2007).
 *
 * <p>Gates :
 * <ul>
 *   <li>workspace.country = BELGIQUE (sinon 400 — outil BE-only) ;</li>
 *   <li>caseFile.legalDomain = DROIT_IMMIGRATION (sinon 400) ;</li>
 *   <li>isolation workspace par {@code WorkspaceMemberRepository} (sinon 404).</li>
 * </ul>
 */
@Service
public class Regroupement10bisBeService {

    private final Regroupement10bisBeRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    /**
     * Seuil de ressources mensuelles paramétrable
     * ({@code regroupement.seuil-ressources-be}). Défaut : 1 500 € (AR 17/05/2007 ~ 120% RIS × 1,5).
     * (à vérifier par avocat BE 2026 — révision annuelle indexation pivot)
     */
    private final int seuilRessources;

    public Regroupement10bisBeService(
            Regroupement10bisBeRepository repository,
            CaseFileRepository caseFileRepository,
            WorkspaceMemberRepository workspaceMemberRepository,
            CurrentUserResolver currentUserResolver,
            ObjectMapper objectMapper,
            @Value("${regroupement.seuil-ressources-be:1500}") int seuilRessources) {
        this.repository = repository;
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
        this.objectMapper = objectMapper;
        this.seuilRessources = seuilRessources;
    }

    @Transactional
    public Regroupement10bisBeResponse calculate(UUID caseFileId,
                                                 Regroupement10bisBeRequest request,
                                                 OidcUser oidcUser,
                                                 Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Regroupement 10bis BE uniquement — pour la France voir les outils "
                            + "regroupement familial CESEDA L.434-2 et seq.");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        Regroupement10bisBeResult result;
        try {
            result = Regroupement10bisBeCalculator.compute(
                    request.lienFamilial(),
                    request.typeCarteRegroupant(),
                    request.revenusMensuelsNetsRegroupant(),
                    request.dureeSejour(),
                    request.dateFinCarteA(),
                    request.logementConforme(),
                    request.assuranceMaladie(),
                    request.menaceOrdrePublic(),
                    seuilRessources);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        Regroupement10bisBeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    Regroupement10bisBeAnalysis a = new Regroupement10bisBeAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setLienFamilial(result.lienFamilial());
        entity.setTypeCarteRegroupant(result.typeCarteRegroupant());
        entity.setRevenusMensuelsNetsRegroupant(result.revenusMensuelsNetsRegroupant());
        entity.setDureeSejour(result.dureeSejour());
        entity.setDateFinCarteA(result.dateFinCarteA());
        entity.setConditionTitreEnCours(result.conditionTitreEnCours());
        entity.setLogementConforme(result.logementConforme());
        entity.setAssuranceMaladie(result.assuranceMaladie());
        entity.setMenaceOrdrePublic(result.menaceOrdrePublic());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, result);
    }

    @Transactional(readOnly = true)
    public Regroupement10bisBeResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        Regroupement10bisBeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse regroupement 10bis BE trouvée pour ce dossier"));
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

    private Regroupement10bisBeResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, Regroupement10bisBeResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private Regroupement10bisBeResponse toResponse(UUID caseFileId, Regroupement10bisBeResult r) {
        return new Regroupement10bisBeResponse(
                caseFileId,
                r.lienFamilial(),
                r.typeCarteRegroupant(),
                r.revenusMensuelsNetsRegroupant(),
                r.dureeSejour(),
                r.dateFinCarteA(),
                r.conditionTitreEnCours(),
                r.logementConforme(),
                r.assuranceMaladie(),
                r.menaceOrdrePublic(),
                r.seuilRessources(),
                r.differentielRevenus(),
                r.scoreEligibilite(),
                r.verdict(),
                r.criteresNonRemplis() != null ? r.criteresNonRemplis() : java.util.List.of(),
                r.baseJuridique()
        );
    }
}
