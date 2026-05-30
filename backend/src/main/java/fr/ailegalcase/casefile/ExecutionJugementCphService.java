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
 * SF-218-03 : service applicatif de l'outil "Exécution du jugement CPH" —
 * exécution forcée d'un jugement du Conseil de prud'hommes et détection de la
 * garantie AGS lorsque l'employeur est en redressement / liquidation judiciaire
 * (art. 514 CPC ; R. 1454-28 CPC ; L. 3253-6 et s. Code travail). Outil
 * <b>FRANCE UNIQUEMENT</b>.
 *
 * <p>Gates :
 * <ul>
 *   <li>workspace.country = FRANCE (sinon 400 — outil FR-only) ;</li>
 *   <li>caseFile.legalDomain = DROIT_DU_TRAVAIL (sinon 400) ;</li>
 *   <li>dateJugement requise / non future, montantCondamnation &gt; 0,
 *       situationEmployeur requise, date d'ouverture requise si procédure
 *       collective (sinon 400) ;</li>
 *   <li>isolation workspace par {@code WorkspaceMemberRepository} (sinon 404).</li>
 * </ul>
 */
@Service
public class ExecutionJugementCphService {

    private final ExecutionJugementCphRepository repository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public ExecutionJugementCphService(ExecutionJugementCphRepository repository,
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
    public ExecutionJugementCphResponse analyze(UUID caseFileId, ExecutionJugementCphRequest request,
                                                OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;
        if (!"FRANCE".equals(country)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Exécution du jugement CPH (art. 514 CPC) — outil FRANCE uniquement");
        }

        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Corps de requête requis");
        }
        if (request.situationEmployeur() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "situationEmployeur est requise");
        }
        boolean procedureCollective =
                request.situationEmployeur() == ExecutionJugementCphSituationEmployeur.REDRESSEMENT
                        || request.situationEmployeur() == ExecutionJugementCphSituationEmployeur.LIQUIDATION;
        if (procedureCollective && request.dateOuvertureProcedureCollective() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "dateOuvertureProcedureCollective est requise en cas de redressement / liquidation");
        }

        ExecutionJugementCphResult result;
        try {
            result = ExecutionJugementCphAnalyzer.analyze(
                    request.dateJugement(),
                    request.montantCondamnation(),
                    request.executionProvisoireOrdonnee(),
                    request.situationEmployeur(),
                    request.dateOuvertureProcedureCollective(),
                    request.ancienneteContratMois(),
                    request.creancesSuperPrivilegiees());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }

        ExecutionJugementCphAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseGet(() -> {
                    ExecutionJugementCphAnalysis a = new ExecutionJugementCphAnalysis();
                    a.setCaseFile(caseFile);
                    return a;
                });
        entity.setDateJugement(result.dateJugement());
        entity.setMontantCondamnation(result.montantCondamnation());
        entity.setExecutionProvisoireOrdonnee(result.executionProvisoireOrdonnee());
        entity.setSituationEmployeur(result.situationEmployeur());
        entity.setDateOuvertureProcedureCollective(result.dateOuvertureProcedureCollective());
        entity.setVerdict(result.verdict());
        entity.setAgsEligible(result.agsEligible());
        entity.setCountry(country);
        entity.setResultData(serialize(result));
        repository.save(entity);

        return toResponse(caseFileId, country, result);
    }

    @Transactional(readOnly = true)
    public ExecutionJugementCphResponse get(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = resolveUser(oidcUser, principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);
        ExecutionJugementCphAnalysis entity = repository.findByCaseFileId(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Aucune analyse d'exécution de jugement CPH trouvée pour ce dossier"));
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

    private ExecutionJugementCphResult deserialize(String json) {
        try {
            return objectMapper.readValue(json, ExecutionJugementCphResult.class);
        } catch (JsonProcessingException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Erreur de désérialisation");
        }
    }

    private ExecutionJugementCphResponse toResponse(UUID caseFileId, String country,
                                                    ExecutionJugementCphResult r) {
        return new ExecutionJugementCphResponse(
                caseFileId,
                r.dateJugement(),
                r.montantCondamnation(),
                r.executionProvisoireOrdonnee(),
                r.situationEmployeur(),
                r.dateOuvertureProcedureCollective(),
                r.ancienneteContratMois(),
                r.creancesSuperPrivilegiees(),
                r.verdict(),
                r.agsEligible(),
                r.relaisAgsRecommande(),
                r.agsCoefficientPlafond(),
                r.agsPlafondEuros(),
                r.agsPlafondMensuelSs(),
                r.checklist(),
                country,
                r.baseJuridique());
    }
}
