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
 * SF-218-11 : service applicatif de l'outil "VRP — indemnité de clientèle" —
 * rupture du contrat d'un VRP statutaire (statut, préavis art. L.7313-9 CT,
 * indemnité de clientèle art. L.7313-13 CT, non-cumul avec l'indemnité légale).
 * Outil <b>FRANCE UNIQUEMENT</b> (régime VRP du droit français).
 *
 * <p>Gates :
 * <ul>
 *   <li>workspace.country = FRANCE (sinon 400 — outil FR-only) ;</li>
 *   <li>caseFile.legalDomain = DROIT_DU_TRAVAIL (sinon 400) ;</li>
 *   <li>dates cohérentes, causeRupture requise, montants ≥ 0 (sinon 400) ;</li>
 *   <li>isolation workspace par {@code WorkspaceMemberRepository} (sinon 404).</li>
 * </ul>
 */
@Service
public class VrpIndemniteClienteleService {

    private final VrpIndemniteClienteleRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public VrpIndemniteClienteleService(VrpIndemniteClienteleRepository repository,
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
    public VrpIndemniteClienteleResponse analyze(UUID caseFileId, VrpIndemniteClienteleRequest request,
                                                 OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "VRP — indemnité de clientèle (art. L.7313-13 CT) — outil FRANCE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        VrpIndemniteClienteleResult result;
        try {
            result = VrpIndemniteClienteleAnalyzer.analyze(
                    request.dateEntree(),
                    request.dateRupture(),
                    request.causeRupture(),
                    request.typeVrp(),
                    request.commissionsAnnuellesMoyennes(),
                    request.salaireMensuelMoyen(),
                    request.clienteleDeveloppee() == null || request.clienteleDeveloppee());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        VrpIndemniteClienteleAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    VrpIndemniteClienteleAnalysis a = new VrpIndemniteClienteleAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDateEntree(result.dateEntree());
        entity.setDateRupture(result.dateRupture());
        entity.setCauseRupture(result.causeRupture());
        entity.setTypeVrp(result.typeVrp());
        entity.setCommissionsAnnuellesMoyennes(result.commissionsAnnuellesMoyennes());
        entity.setSalaireMensuelMoyen(result.salaireMensuelMoyen());
        entity.setClienteleDeveloppee(result.clienteleDeveloppee());
        entity.setDureePreavisMois(result.dureePreavisMois());
        entity.setEligibiliteClientele(result.eligibiliteClientele());
        entity.setOptionRecommandee(result.optionRecommandee());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public VrpIndemniteClienteleResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        VrpIndemniteClienteleAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse VRP indemnité de clientèle trouvée pour ce dossier"));
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

    private VrpIndemniteClienteleResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, VrpIndemniteClienteleResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private VrpIndemniteClienteleResponse toResponse(UUID caseFileId, String country,
                                                     VrpIndemniteClienteleResult r) {
        return new VrpIndemniteClienteleResponse(
                caseFileId,
                r.dateEntree(),
                r.dateRupture(),
                r.causeRupture(),
                r.typeVrp(),
                r.commissionsAnnuellesMoyennes(),
                r.salaireMensuelMoyen(),
                r.clienteleDeveloppee(),
                r.ancienneteMois(),
                r.dureePreavisMois(),
                r.eligibiliteClientele(),
                r.motifNonDue(),
                r.indemniteClienteleMin(),
                r.indemniteClienteleMax(),
                r.indemniteLegaleLicenciement(),
                r.optionRecommandee(),
                country,
                r.baseJuridique());
    }
}
