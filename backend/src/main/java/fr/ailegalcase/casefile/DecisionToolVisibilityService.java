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
     *
     * <p>F-197 SF-197-01 — applique l'override avocat en priorité sur les valeurs
     * IA brutes pour les champs {@code type_litige_detecte} (Travail FR) et
     * {@code type_procedure_detectee} (Immigration). Les autres champs restent
     * inchangés (lecture IA pure).</p>
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

        // F-197 SF-197-01 — type_procedure : override avocat prioritaire sur IA.
        String typeProcedureOverride = latest.getTypeProcedureAvocatOverride();
        if (typeProcedureOverride != null && !typeProcedureOverride.isBlank()) {
            addIfPresent(detected, "type_procedure_detectee", typeProcedureOverride);
        } else {
            addIfPresent(detected, "type_procedure_detectee", readString(root.path("type_procedure_detectee")));
        }

        // F-197 SF-197-01 — type_litige : override avocat prioritaire sur IA.
        // L'override est aussi propagé vers les trigger_field réellement utilisés par
        // les règles de visibilité (type_rupture, motif_nullite_pressenti, etc.) pour
        // que F-DT-11/12/13/20 etc. s'activent même si l'IA n'a pas peuplé ces champs
        // ou les a peuplés différemment.
        String typeLitigeOverride = latest.getTypeLitigeAvocatOverride();
        if (typeLitigeOverride != null && !typeLitigeOverride.isBlank()) {
            addIfPresent(detected, "type_litige_detecte", typeLitigeOverride);
            propagateTypeLitigeOverrideTriggers(detected, typeLitigeOverride);
        } else {
            addIfPresent(detected, "type_litige_detecte", readString(root.path("type_litige_detecte")));
        }

        addIfPresent(detected, "type_recours_code", readString(root.path("type_recours_code")));
        addIfPresent(detected, "type_titre_sejour_code", readString(root.path("type_titre_sejour_code")));
        addIfPresent(detected, "regime_matrimonial",
                readString(root.path("liquidation_communaute_data").path("regime_matrimonial")));
        addIfPresent(detected, "mode_garde_detaille",
                readString(root.path("pension_alimentaire_data").path("mode_garde_detaille")));

        // F-165 SF-165-01 : 5 nouveaux trigger_field lus depuis travail_extracted_data
        // pour basculer 14 outils Travail FR ALWAYS_ON → CONTEXTUAL.
        JsonNode travailNode = root.path("travail_extracted_data");
        addIfPresent(detected, "type_contrat", readString(travailNode.path("type_contrat")));
        addIfPresent(detected, "motif_licenciement", readString(travailNode.path("motif_licenciement")));
        addIfPresent(detected, "origine_inaptitude_pressentie",
                readString(travailNode.path("origine_inaptitude_pressentie")));
        addIfPresent(detected, "motif_nullite_pressenti",
                readString(travailNode.path("motif_nullite_pressenti")));
        // heures_sup_mentionnees est un objet JSON ({total_declarees_25pct, ...} ou null) ;
        // on émet le trigger "PRESENT" dès qu'au moins le node est un objet non vide.
        JsonNode heuresSupNode = travailNode.path("heures_sup_mentionnees");
        if (heuresSupNode != null && heuresSupNode.isObject() && !heuresSupNode.isEmpty()) {
            addIfPresent(detected, "heures_sup_mentionnees", "PRESENT");
        }

        // F-166 SF-166-02 : 8 flags décisionnels niveau 3 — booleans dans travail_extracted_data.
        // Émettre la string "true" dans la map quand le flag est à true (consommé par CONTEXTUAL
        // rules de la migration 199 : F-DT-20/21/24/30/31/33/34/35).
        addBooleanFlagIfTrue(detected, travailNode, "rappel_salaire_detecte");
        addBooleanFlagIfTrue(detected, travailNode, "travail_dissimule_detecte");
        addBooleanFlagIfTrue(detected, travailNode, "clause_non_concurrence_detectee");
        addBooleanFlagIfTrue(detected, travailNode, "statut_protege_detecte");
        addBooleanFlagIfTrue(detected, travailNode, "transaction_envisagee");
        addBooleanFlagIfTrue(detected, travailNode, "at_mp_detecte");
        addBooleanFlagIfTrue(detected, travailNode, "urgence_procedurale");
        addBooleanFlagIfTrue(detected, travailNode, "contestation_are_envisagee");

        // F-204 : 5 flags Travail BE — booleans dans travail_extracted_data.
        // Migration 215 : F-DT-11/12/15/19/27 BE basculent ALWAYS_ON → CONTEXTUAL.
        addBooleanFlagIfTrue(detected, travailNode, "harcelement_be_detecte");
        addBooleanFlagIfTrue(detected, travailNode, "discrimination_be_detectee");
        addBooleanFlagIfTrue(detected, travailNode, "inaptitude_medicale_be_detectee");
        addBooleanFlagIfTrue(detected, travailNode, "heures_sup_mentionnees_be");
        addBooleanFlagIfTrue(detected, travailNode, "motif_grave_be_envisage");

        // F-201 / F-203 : 9 flags Immigration FR + 5 flags Immigration BE.
        // Migrations 213 + 214 : 14 outils Immigration FR/BE basculent ALWAYS_ON → CONTEXTUAL.
        JsonNode immigrationNode = root.path("immigration_extracted_data");
        // F-201 (9 flags FR)
        addBooleanFlagIfTrue(detected, immigrationNode, "aes_metiers_tension_eligible_detecte");
        addBooleanFlagIfTrue(detected, immigrationNode, "aes_familial_eligible_detecte");
        addBooleanFlagIfTrue(detected, immigrationNode, "aes_humanitaire_eligible_detecte");
        addBooleanFlagIfTrue(detected, immigrationNode, "aes_etudiant_eligible_detecte");
        addBooleanFlagIfTrue(detected, immigrationNode, "changement_statut_envisage_detecte");
        addBooleanFlagIfTrue(detected, immigrationNode, "procedure_asile_detectee");
        addBooleanFlagIfTrue(detected, immigrationNode, "naturalisation_envisagee_detectee");
        addBooleanFlagIfTrue(detected, immigrationNode, "client_mineur_detecte");
        addBooleanFlagIfTrue(detected, immigrationNode, "mesure_eloignement_detectee");
        // F-203 (5 flags BE)
        addBooleanFlagIfTrue(detected, immigrationNode, "procedure_9bis_envisagee");
        addBooleanFlagIfTrue(detected, immigrationNode, "procedure_9ter_medicale_detectee");
        addBooleanFlagIfTrue(detected, immigrationNode, "regroupement_40bis_detecte");
        addBooleanFlagIfTrue(detected, immigrationNode, "regroupement_40ter_detecte");
        addBooleanFlagIfTrue(detected, immigrationNode, "oqt_annexe13_detectee");
        return detected;
    }

    /**
     * F-197 SF-197-01 — propage l'override {@code type_litige_avocat_override} vers les
     * trigger_field réellement utilisés par les règles de visibilité Travail FR
     * (cf. migrations 193 + 199), pour que les outils {@code F-DT-11/12/13/20/21/24/...}
     * s'activent quand l'avocat surcharge la classification IA.
     *
     * <p>Mapping :
     * <ul>
     *   <li>LICENCIEMENT_SANS_CAUSE_REELLE → {@code type_rupture=LICENCIEMENT}</li>
     *   <li>LICENCIEMENT_ECONOMIQUE → {@code type_rupture=LICENCIEMENT_ECONOMIQUE}</li>
     *   <li>PRISE_ACTE_RUPTURE → {@code type_rupture=PRISE_ACTE}</li>
     *   <li>HARCELEMENT_MORAL → {@code motif_nullite_pressenti=HARCELEMENT_MORAL}</li>
     *   <li>DISCRIMINATION → {@code motif_nullite_pressenti=DISCRIMINATION}</li>
     *   <li>HEURES_SUPPLEMENTAIRES → {@code heures_sup_mentionnees=PRESENT}</li>
     *   <li>RAPPEL_SALAIRE → {@code rappel_salaire_detecte=true}</li>
     * </ul>
     */
    private static void propagateTypeLitigeOverrideTriggers(Map<String, Set<String>> detected,
                                                             String typeLitigeOverride) {
        switch (typeLitigeOverride) {
            case "LICENCIEMENT_SANS_CAUSE_REELLE" -> addIfPresent(detected, "type_rupture", "LICENCIEMENT");
            case "LICENCIEMENT_ECONOMIQUE" -> addIfPresent(detected, "type_rupture", "LICENCIEMENT_ECONOMIQUE");
            case "PRISE_ACTE_RUPTURE" -> addIfPresent(detected, "type_rupture", "PRISE_ACTE");
            case "HARCELEMENT_MORAL" -> addIfPresent(detected, "motif_nullite_pressenti", "HARCELEMENT_MORAL");
            case "DISCRIMINATION" -> addIfPresent(detected, "motif_nullite_pressenti", "DISCRIMINATION");
            case "HEURES_SUPPLEMENTAIRES" -> addIfPresent(detected, "heures_sup_mentionnees", "PRESENT");
            case "RAPPEL_SALAIRE" -> addIfPresent(detected, "rappel_salaire_detecte", "true");
            default -> { /* no-op */ }
        }
    }

    /**
     * F-166 SF-166-02 : émet la string "true" dans la map detected[field] quand le node JSON
     * contient le champ avec la valeur boolean true (ou string "true"). Skip silencieux si le
     * champ est absent, null, false, ou non-boolean — comportement aligné sur le helper
     * {@code booleanOrFalse} côté CaseAnalysisResponse (SF-166-01).
     */
    private static void addBooleanFlagIfTrue(Map<String, Set<String>> detected, JsonNode parent, String field) {
        if (parent == null || !parent.has(field)) return;
        JsonNode v = parent.get(field);
        if (v == null || v.isNull()) return;
        boolean isTrue;
        if (v.isBoolean()) {
            isTrue = v.asBoolean();
        } else if (v.isTextual()) {
            isTrue = "true".equalsIgnoreCase(v.asText());
        } else {
            isTrue = false;
        }
        if (isTrue) {
            addIfPresent(detected, field, "true");
        }
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
