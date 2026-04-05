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
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ProcedureCheckService {

    private static final Logger log = LoggerFactory.getLogger(ProcedureCheckService.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final ProcedureCheckRepository procedureCheckRepository;
    private final CaseAnalysisRepository caseAnalysisRepository;
    private final CaseFileRepository caseFileRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ApplicationEventPublisher eventPublisher;

    public ProcedureCheckService(ProcedureCheckRepository procedureCheckRepository,
                                 CaseAnalysisRepository caseAnalysisRepository,
                                 CaseFileRepository caseFileRepository,
                                 WorkspaceMemberRepository workspaceMemberRepository,
                                 CurrentUserResolver currentUserResolver,
                                 ApplicationEventPublisher eventPublisher) {
        this.procedureCheckRepository = procedureCheckRepository;
        this.caseAnalysisRepository = caseAnalysisRepository;
        this.caseFileRepository = caseFileRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
        this.eventPublisher = eventPublisher;
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
     * Crée les procedure_checks pour une analyse enrichie.
     * - Propage les NON_COMPLIANT de l'analyse précédente (fix #3) : si le libellé correspond à un TO_CHECK
     *   nouvellement créé, celui-ci est upgradé en NON_COMPLIANT ; sinon ajouté en fin de liste.
     * - Propage les VERIFIED non requalifiés, sans doublon avec les checks déjà présents (fix #1/#2).
     * - Matching normalisé (trim + lowercase) pour les requalifications et la déduplication (fix #4).
     * Fail-open sur toutes les opérations de propagation.
     */
    @Transactional
    public void createChecksWithVerifiedPropagation(CaseAnalysis newAnalysis, String rawJson,
                                                     UUID previousAnalysisId) {
        // 1. Créer les nouveaux checks TO_CHECK depuis points_procedure
        createChecks(newAnalysis, rawJson);

        if (previousAnalysisId == null) return;

        String stripped = CaseAnalysisResponse.stripMarkdownCodeBlock(rawJson != null ? rawJson : "");
        List<CheckRequalification> requalifications = parseRequalifications(stripped, newAnalysis.getId());

        List<ProcedureCheck> verifiedChecks = procedureCheckRepository
                .findByCaseAnalysisIdAndStatutOrderByOrdreAsc(previousAnalysisId, ProcedureCheckStatus.VERIFIED);
        List<ProcedureCheck> nonCompliantChecks = procedureCheckRepository
                .findByCaseAnalysisIdAndStatutOrderByOrdreAsc(previousAnalysisId, ProcedureCheckStatus.NON_COMPLIANT);

        if (verifiedChecks.isEmpty() && nonCompliantChecks.isEmpty()) return;

        // 2. Appliquer les requalifications sur les VERIFIED de l'analyse précédente (matching normalisé)
        List<ProcedureCheckRequalifiedEvent.RequalifiedCheck> effective = new ArrayList<>();
        for (CheckRequalification req : requalifications) {
            verifiedChecks.stream()
                    .filter(c -> normalize(c.getDescription()).equals(normalize(req.description())))
                    .findFirst()
                    .ifPresent(c -> {
                        c.setStatut(req.newStatut());
                        c.setRaison(req.raison());
                        procedureCheckRepository.save(c);
                        effective.add(new ProcedureCheckRequalifiedEvent.RequalifiedCheck(
                                c.getDescription(), req.newStatut(), req.raison()));
                    });
        }

        if (!effective.isEmpty()) {
            try {
                UUID caseFileId = newAnalysis.getCaseFile().getId();
                String title = caseFileRepository.findTitleById(caseFileId).orElse(null);
                String creatorEmail = caseFileRepository.findCreatorEmailById(caseFileId).orElse(null);
                if (title != null && creatorEmail != null) {
                    eventPublisher.publishEvent(
                            new ProcedureCheckRequalifiedEvent(caseFileId, title, creatorEmail, effective));
                }
            } catch (Exception e) {
                log.warn("Failed to publish ProcedureCheckRequalifiedEvent for analysis {} — {}", newAnalysis.getId(), e.getMessage());
            }
        }

        // 3. Construire l'index des descriptions déjà présentes dans la nouvelle analyse
        List<ProcedureCheck> currentChecks = procedureCheckRepository
                .findByCaseAnalysisIdOrderByOrdreAsc(newAnalysis.getId());
        Set<String> existingNormalized = currentChecks.stream()
                .map(c -> normalize(c.getDescription()))
                .collect(Collectors.toSet());
        int offset = currentChecks.size();
        Workspace workspace = newAnalysis.getCaseFile().getWorkspace();

        // 4. Propager les NON_COMPLIANT : upgrader le TO_CHECK existant si même libellé, sinon ajouter
        for (ProcedureCheck src : nonCompliantChecks) {
            String norm = normalize(src.getDescription());
            if (existingNormalized.contains(norm)) {
                currentChecks.stream()
                        .filter(c -> normalize(c.getDescription()).equals(norm)
                                && c.getStatut() == ProcedureCheckStatus.TO_CHECK)
                        .findFirst()
                        .ifPresent(c -> {
                            c.setStatut(ProcedureCheckStatus.NON_COMPLIANT);
                            procedureCheckRepository.save(c);
                        });
            } else {
                ProcedureCheck copy = new ProcedureCheck();
                copy.setCaseAnalysis(newAnalysis);
                copy.setWorkspace(workspace);
                copy.setOrdre(offset++);
                copy.setDescription(src.getDescription());
                copy.setStatut(ProcedureCheckStatus.NON_COMPLIANT);
                copy.setRaison(src.getRaison());
                procedureCheckRepository.save(copy);
                existingNormalized.add(norm);
            }
        }

        // 5. Propager les VERIFIED encore intacts, sans doublon
        for (ProcedureCheck src : verifiedChecks) {
            if (src.getStatut() != ProcedureCheckStatus.VERIFIED) continue;
            String norm = normalize(src.getDescription());
            if (existingNormalized.contains(norm)) continue;
            ProcedureCheck copy = new ProcedureCheck();
            copy.setCaseAnalysis(newAnalysis);
            copy.setWorkspace(workspace);
            copy.setOrdre(offset++);
            copy.setDescription(src.getDescription());
            copy.setStatut(ProcedureCheckStatus.VERIFIED);
            copy.setRaison(src.getRaison());
            procedureCheckRepository.save(copy);
            existingNormalized.add(norm);
        }
    }

    private static String normalize(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }

    private record CheckRequalification(String description, ProcedureCheckStatus newStatut, String raison) {}

    private List<CheckRequalification> parseRequalifications(String stripped, UUID analysisId) {
        List<CheckRequalification> result = new ArrayList<>();
        try {
            JsonNode root = MAPPER.readTree(stripped);
            JsonNode node = root.get("checks_a_requalifier");
            if (node == null || !node.isArray()) return result;
            for (JsonNode item : node) {
                String desc = item.has("description") ? item.get("description").asText(null) : null;
                String statutStr = item.has("nouveau_statut") ? item.get("nouveau_statut").asText(null) : null;
                String raison = item.has("raison") ? item.get("raison").asText(null) : null;
                if (desc == null || statutStr == null) continue;
                try {
                    ProcedureCheckStatus statut = ProcedureCheckStatus.valueOf(statutStr);
                    if (statut == ProcedureCheckStatus.VERIFIED) continue; // invalide
                    result.add(new CheckRequalification(desc, statut, raison));
                } catch (IllegalArgumentException e) {
                    log.debug("checks_a_requalifier statut invalide '{}' pour analysis {} — ignoré", statutStr, analysisId);
                }
            }
        } catch (Exception e) {
            log.debug("checks_a_requalifier extraction failed for analysis {} — skipping", analysisId);
        }
        return result;
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
     * Retourne les descriptions des checks VERIFIED de la dernière analyse DONE du dossier.
     * Fail-open : toute exception retourne une liste vide.
     */
    @Transactional(readOnly = true)
    public List<String> listVerified(CaseFile caseFile) {
        try {
            return caseAnalysisRepository
                    .findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFile.getId(), AnalysisStatus.DONE)
                    .map(analysis -> procedureCheckRepository
                            .findByCaseAnalysisIdAndStatutOrderByOrdreAsc(analysis.getId(), ProcedureCheckStatus.VERIFIED)
                            .stream().map(ProcedureCheck::getDescription).toList())
                    .orElse(List.of());
        } catch (Exception e) {
            log.warn("listVerified failed for caseFile {} — skipping", caseFile.getId(), e);
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
