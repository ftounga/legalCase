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
 * SF-214-19 : service applicatif de l'outil "AJ CNDA — éligibilité & délais"
 * (loi n° 91-647, L. 532-4 CESEDA). Outil <b>FRANCE UNIQUEMENT</b> (droit d'asile).
 *
 * <p>Gates :
 * <ul>
 *   <li>workspace.country = FRANCE (sinon 400 — outil FR-only) ;</li>
 *   <li>caseFile.legalDomain = DROIT_IMMIGRATION (sinon 400) ;</li>
 *   <li>isolation workspace par {@code WorkspaceMemberRepository} (sinon 404).</li>
 * </ul>
 */
@Service
public class AjCndaService {

    private final AjCndaRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public AjCndaService(AjCndaRepository repository,
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
    public AjCndaResponse analyze(UUID caseFileId, AjCndaRequest request,
                                  OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "AJ CNDA (loi 91-647, L. 532-4 CESEDA) — outil FRANCE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }

        AjCndaResult result;
        try {
            result = AjCndaAnalyzer.analyze(
                    request.dateDecisionOFPRA(),
                    request.ressourcesMensuellesNettes(),
                    request.procedureAcceleree(),
                    request.demandeAJDeposee(),
                    request.dateDepotAJ());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        AjCndaAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    AjCndaAnalysis a = new AjCndaAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDateDecisionOfpra(result.dateDecisionOFPRA());
        entity.setRessourcesMensuellesNettes(result.ressourcesMensuellesNettes());
        entity.setProcedureAcceleree(result.procedureAcceleree());
        entity.setDemandeAjDeposee(result.demandeAJDeposee());
        entity.setDateDepotAj(result.dateDepotAJ());
        entity.setEligibleAj(result.eligibleAJ());
        entity.setStatut(result.statut());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public AjCndaResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        AjCndaAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse AJ CNDA trouvée pour ce dossier"));
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

    private AjCndaResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, AjCndaResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private AjCndaResponse toResponse(UUID caseFileId, String country, AjCndaResult r) {
        return new AjCndaResponse(
                caseFileId,
                r.dateDecisionOFPRA(),
                r.ressourcesMensuellesNettes(),
                r.procedureAcceleree(),
                r.demandeAJDeposee(),
                r.dateDepotAJ(),
                r.eligibleAJ(),
                r.dateEcheanceRecoursCNDA(),
                r.dateEcheanceDemandeAJ(),
                r.procedureAccelereeDureeReduite(),
                r.statut(),
                r.piecesAJ(),
                r.recommandation(),
                country,
                r.baseJuridique()
        );
    }
}
