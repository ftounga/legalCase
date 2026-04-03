package fr.ailegalcase.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.casefile.CaseFile;
import fr.ailegalcase.casefile.CaseFileRepository;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.shared.OAuthProviderResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Service
public class ProcedureCheckService {

    private static final Logger log = LoggerFactory.getLogger(ProcedureCheckService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ProcedureCheckRepository procedureCheckRepository;
    private final CaseAnalysisRepository caseAnalysisRepository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;

    public ProcedureCheckService(ProcedureCheckRepository procedureCheckRepository,
                                 CaseAnalysisRepository caseAnalysisRepository,
                                 CaseFileRepository caseFileRepository,
                                 WorkspaceMemberRepository workspaceMemberRepository,
                                 CurrentUserResolver currentUserResolver) {
        this.procedureCheckRepository = procedureCheckRepository;
        this.caseAnalysisRepository = caseAnalysisRepository;
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
    }

    /**
     * Crée les procedure_checks à partir du JSON brut de l'analyse.
     * Fail-open : si points_procedure absent ou JSON invalide, aucun check créé.
     * Remplace les checks existants pour cette analyse.
     */
    @Transactional
    public void createChecks(CaseAnalysis analysis, String rawJson) {
        if (rawJson == null || rawJson.isBlank()) return;

        String stripped = CaseAnalysisResponse.stripMarkdownCodeBlock(rawJson);
        List<String> points;
        try {
            JsonNode root = MAPPER.readTree(stripped);
            JsonNode node = root.get("points_procedure");
            if (node == null || !node.isArray()) return;
            points = new java.util.ArrayList<>();
            for (JsonNode item : node) {
                if (item.isTextual()) points.add(item.asText());
            }
        } catch (Exception e) {
            log.debug("points_procedure extraction failed for analysis {} — skipping", analysis.getId());
            return;
        }

        if (points.isEmpty()) return;

        procedureCheckRepository.deleteByCaseAnalysisId(analysis.getId());

        Workspace workspace = analysis.getCaseFile().getWorkspace();
        for (int i = 0; i < points.size(); i++) {
            ProcedureCheck check = new ProcedureCheck();
            check.setCaseAnalysis(analysis);
            check.setWorkspace(workspace);
            check.setOrdre(i);
            check.setDescription(points.get(i));
            check.setStatut(ProcedureCheckStatus.TO_CHECK);
            procedureCheckRepository.save(check);
        }
    }

    /**
     * Retourne les descriptions des checks NON_COMPLIANT de la dernière analyse DONE du dossier.
     * Fail-open : toute exception retourne une liste vide.
     */
    @Transactional(readOnly = true)
    public List<String> listNonCompliant(CaseFile caseFile) {
        try {
            return caseAnalysisRepository
                    .findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFile.getId(), AnalysisStatus.DONE)
                    .map(analysis -> procedureCheckRepository
                            .findByCaseAnalysisIdAndStatutOrderByOrdreAsc(analysis.getId(), ProcedureCheckStatus.NON_COMPLIANT)
                            .stream().map(ProcedureCheck::getDescription).toList())
                    .orElse(List.of());
        } catch (Exception e) {
            log.warn("listNonCompliant failed for caseFile {} — skipping", caseFile.getId(), e);
            return List.of();
        }
    }

    /**
     * Retourne les descriptions des checks TO_CHECK de la dernière analyse DONE du dossier.
     * Fail-open : toute exception retourne une liste vide.
     */
    @Transactional(readOnly = true)
    public List<String> listToCheck(CaseFile caseFile) {
        try {
            return caseAnalysisRepository
                    .findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFile.getId(), AnalysisStatus.DONE)
                    .map(analysis -> procedureCheckRepository
                            .findByCaseAnalysisIdAndStatutOrderByOrdreAsc(analysis.getId(), ProcedureCheckStatus.TO_CHECK)
                            .stream().map(ProcedureCheck::getDescription).toList())
                    .orElse(List.of());
        } catch (Exception e) {
            log.warn("listToCheck failed for caseFile {} — skipping", caseFile.getId(), e);
            return List.of();
        }
    }

    @Transactional(readOnly = true)
    public List<ProcedureCheckResponse> list(UUID caseFileId, UUID analysisId,
                                              OidcUser oidcUser, Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);
        Workspace workspace = resolveWorkspace(user);
        CaseFile caseFile = resolveCaseFile(caseFileId, workspace);

        CaseAnalysis analysis = caseAnalysisRepository.findById(analysisId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Analysis not found"));

        if (!analysis.getCaseFile().getId().equals(caseFile.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Analysis not found");
        }

        return procedureCheckRepository.findByCaseAnalysisIdOrderByOrdreAsc(analysisId)
                .stream().map(ProcedureCheckResponse::from).toList();
    }

    @Transactional
    public ProcedureCheckResponse updateStatus(UUID checkId, String newStatut,
                                               OidcUser oidcUser, Principal principal) {
        ProcedureCheckStatus status;
        try {
            status = ProcedureCheckStatus.valueOf(newStatut);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statut invalide : " + newStatut);
        }

        User user = currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);
        Workspace workspace = resolveWorkspace(user);

        ProcedureCheck check = procedureCheckRepository.findByIdAndWorkspaceId(checkId, workspace.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Check not found"));

        check.setStatut(status);
        return ProcedureCheckResponse.from(procedureCheckRepository.save(check));
    }

    private Workspace resolveWorkspace(User user) {
        return workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Workspace not found"))
                .getWorkspace();
    }

    private CaseFile resolveCaseFile(UUID caseFileId, Workspace workspace) {
        CaseFile caseFile = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        if (!caseFile.getWorkspace().getId().equals(workspace.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }
        return caseFile;
    }
}
