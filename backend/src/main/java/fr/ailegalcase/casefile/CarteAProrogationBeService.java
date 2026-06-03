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
 * SF-221-01 : service applicatif de l'outil prorogation de la carte A BE
 * (Loi 15/12/1980 art. 13 + AR 08/10/1981 art. 33 — séjour temporaire / limité).
 *
 * <p>Gates :
 * <ul>
 *   <li>workspace.country = BELGIQUE (sinon 400 — outil BE-only) ;</li>
 *   <li>caseFile.legalDomain = DROIT_IMMIGRATION (sinon 400) ;</li>
 *   <li>champ invalide (date absente, dateDemande manquante si demandeDeposee) → 400 ;</li>
 *   <li>isolation workspace par {@code WorkspaceMemberRepository} (sinon 404).</li>
 * </ul>
 */
@Service
public class CarteAProrogationBeService {

    private final CarteAProrogationBeRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public CarteAProrogationBeService(
            CarteAProrogationBeRepository repository,
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
    public CarteAProrogationBeResponse calculate(UUID caseFileId,
                                                 CarteAProrogationBeRequest request,
                                                 OidcUser oidcUser,
                                                 Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"BELGIQUE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Prorogation carte A BE uniquement — pour la France voir les outils "
                            + "de renouvellement de titre de séjour CESEDA.");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        CarteAProrogationBeResult result;
        try {
            result = CarteAProrogationBeCalculator.compute(
                    request.dateExpirationCarteA(),
                    request.motifSejourPersiste(),
                    request.conditionsInitialesToujoursReunies(),
                    request.demandeDeposee(),
                    request.dateDemande());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        CarteAProrogationBeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    CarteAProrogationBeAnalysis a = new CarteAProrogationBeAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDateExpirationCarteA(result.dateExpirationCarteA());
        entity.setMotifSejourPersiste(result.motifSejourPersiste());
        entity.setConditionsInitialesToujoursReunies(result.conditionsInitialesToujoursReunies());
        entity.setDemandeDeposee(result.demandeDeposee());
        entity.setDateDemande(result.dateDemande());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, result);
    }

    @Transactional(readOnly = true)
    public CarteAProrogationBeResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        resolveCaseFile(caseFileId, user);
        CarteAProrogationBeAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse prorogation carte A BE trouvée pour ce dossier"));
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

    private CarteAProrogationBeResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, CarteAProrogationBeResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private CarteAProrogationBeResponse toResponse(UUID caseFileId, CarteAProrogationBeResult r) {
        return new CarteAProrogationBeResponse(
                caseFileId,
                r.dateExpirationCarteA(),
                r.motifSejourPersiste(),
                r.conditionsInitialesToujoursReunies(),
                r.demandeDeposee(),
                r.dateDemande(),
                r.verdict(),
                r.joursAvantExpiration(),
                r.dateOuvertureFenetre(),
                r.dateLimiteRecommandee(),
                r.basesJuridiques() != null ? r.basesJuridiques() : java.util.List.of(),
                r.messages() != null ? r.messages() : java.util.List.of());
    }
}
