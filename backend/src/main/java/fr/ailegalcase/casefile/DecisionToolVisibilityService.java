package fr.ailegalcase.casefile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.analysis.AnalysisStatus;
import fr.ailegalcase.analysis.CaseAnalysis;
import fr.ailegalcase.analysis.CaseAnalysisRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.shared.OAuthProviderResolver;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * SF-IA-04-01 : résout la liste d'outils décisionnels à afficher pour un dossier
 * donné, en trois couches (alwaysOn / contextual / catalog).
 *
 * Pur read-only : lit la configuration {@link DecisionToolVisibilityRule} et les
 * codes de situation détectés par l'IA dans la dernière analyse DONE. Ne modifie
 * aucun état.
 */
@Service
public class DecisionToolVisibilityService {

    private static final Logger log = LoggerFactory.getLogger(DecisionToolVisibilityService.class);

    private final DecisionToolVisibilityRuleRepository ruleRepository;
    private final CaseFileRepository caseFileRepository;
    private final CaseAnalysisRepository caseAnalysisRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final CurrentUserResolver currentUserResolver;
    private final ObjectMapper objectMapper;

    public DecisionToolVisibilityService(DecisionToolVisibilityRuleRepository ruleRepository,
                                         CaseFileRepository caseFileRepository,
                                         CaseAnalysisRepository caseAnalysisRepository,
                                         WorkspaceMemberRepository workspaceMemberRepository,
                                         CurrentUserResolver currentUserResolver,
                                         ObjectMapper objectMapper) {
        this.ruleRepository = ruleRepository;
        this.caseFileRepository = caseFileRepository;
        this.caseAnalysisRepository = caseAnalysisRepository;
        this.workspaceMemberRepository = workspaceMemberRepository;
        this.currentUserResolver = currentUserResolver;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public VisibleToolSetResponse resolveVisibleTools(UUID caseFileId, OidcUser oidcUser, Principal principal) {
        User user = currentUserResolver.resolve(oidcUser, OAuthProviderResolver.resolve(principal), principal);
        CaseFile caseFile = resolveCaseFile(caseFileId, user);

        String legalDomain = caseFile.getLegalDomain();
        String country = caseFile.getWorkspace() != null ? caseFile.getWorkspace().getCountry() : null;

        if (legalDomain == null) {
            log.warn("CaseFile {} has null legalDomain — returning empty visibility set", caseFileId);
            return new VisibleToolSetResponse(List.of(), List.of(), List.of());
        }

        List<DecisionToolVisibilityRule> rules = ruleRepository.findForDomainAndCountry(legalDomain, country);
        Map<String, Set<String>> detectedSituations = extractDetectedSituations(caseFileId);

        return buildResponse(rules, detectedSituations);
    }

    private CaseFile resolveCaseFile(UUID caseFileId, User user) {
        CaseFile cf = caseFileRepository.findByIdAndDeletedAtIsNull(caseFileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found"));
        UUID userWorkspaceId = workspaceMemberRepository.findByUserAndPrimaryTrue(user)
                .map(m -> m.getWorkspace().getId())
                .orElse(null);
        if (userWorkspaceId == null || !userWorkspaceId.equals(cf.getWorkspace().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Case file not found");
        }
        return cf;
    }

    /**
     * Résout les codes de situation détectés par l'IA sur la dernière analyse
     * DONE du dossier. Retourne une map `trigger_field -> set de trigger_values`.
     *
     * Tolère l'absence d'analyse (map vide) et les JSON mal formés (skip + log).
     */
    private Map<String, Set<String>> extractDetectedSituations(UUID caseFileId) {
        CaseAnalysis latest = caseAnalysisRepository
                .findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(caseFileId, AnalysisStatus.DONE)
                .orElse(null);
        if (latest == null || latest.getAnalysisResult() == null || latest.getAnalysisResult().isBlank()) {
            return Map.of();
        }
        JsonNode root;
        try {
            root = objectMapper.readTree(latest.getAnalysisResult());
        } catch (Exception e) {
            log.warn("CaseAnalysis {} for caseFile {} has invalid JSON — skipping detection", latest.getId(), caseFileId);
            return Map.of();
        }

        Map<String, Set<String>> detected = new HashMap<>();
        addIfPresent(detected, "type_rupture", readString(root.path("compensation_data").path("type_rupture")));
        addIfPresent(detected, "type_rupture", readString(root.path("type_rupture")));
        addIfPresent(detected, "type_procedure_detectee", readString(root.path("type_procedure_detectee")));
        addIfPresent(detected, "type_recours_code", readString(root.path("type_recours_code")));
        addIfPresent(detected, "type_titre_sejour_code", readString(root.path("type_titre_sejour_code")));
        addIfPresent(detected, "regime_matrimonial",
                readString(root.path("liquidation_communaute_data").path("regime_matrimonial")));
        addIfPresent(detected, "mode_garde_detaille",
                readString(root.path("pension_alimentaire_data").path("mode_garde_detaille")));
        return detected;
    }

    private static String readString(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String s = node.asText();
        return (s == null || s.isBlank()) ? null : s;
    }

    private static void addIfPresent(Map<String, Set<String>> map, String field, String value) {
        if (value == null) {
            return;
        }
        map.computeIfAbsent(field, k -> new HashSet<>()).add(value);
    }

    private VisibleToolSetResponse buildResponse(List<DecisionToolVisibilityRule> rules,
                                                 Map<String, Set<String>> detected) {
        Comparator<DecisionToolVisibilityRule> byPriorityThenId =
                Comparator.comparingInt(DecisionToolVisibilityRule::getPriority)
                        .thenComparing(DecisionToolVisibilityRule::getToolId);

        Set<String> alwaysOn = new LinkedHashSet<>();
        rules.stream()
                .filter(r -> r.getLayer() == DecisionToolVisibilityRule.Layer.ALWAYS_ON)
                .sorted(byPriorityThenId)
                .forEach(r -> alwaysOn.add(r.getToolId()));

        Set<String> contextual = new LinkedHashSet<>();
        rules.stream()
                .filter(r -> r.getLayer() == DecisionToolVisibilityRule.Layer.CONTEXTUAL)
                .filter(r -> {
                    Set<String> values = detected.get(r.getTriggerField());
                    return values != null && values.contains(r.getTriggerValue());
                })
                .sorted(byPriorityThenId)
                .forEach(r -> contextual.add(r.getToolId()));

        Set<String> allContextualTools = new LinkedHashSet<>();
        rules.stream()
                .filter(r -> r.getLayer() == DecisionToolVisibilityRule.Layer.CONTEXTUAL)
                .map(DecisionToolVisibilityRule::getToolId)
                .sorted()
                .forEach(allContextualTools::add);

        List<String> catalog = new ArrayList<>();
        for (String toolId : allContextualTools) {
            if (!contextual.contains(toolId)) {
                catalog.add(toolId);
            }
        }

        return new VisibleToolSetResponse(
                new ArrayList<>(alwaysOn),
                new ArrayList<>(contextual),
                catalog);
    }
}
