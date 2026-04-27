package fr.ailegalcase.casefile;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.analysis.AnalysisStatus;
import fr.ailegalcase.analysis.CaseAnalysis;
import fr.ailegalcase.analysis.CaseAnalysisRepository;
import fr.ailegalcase.auth.User;
import fr.ailegalcase.shared.CurrentUserResolver;
import fr.ailegalcase.workspace.Workspace;
import fr.ailegalcase.workspace.WorkspaceMember;
import fr.ailegalcase.workspace.WorkspaceMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static fr.ailegalcase.casefile.DecisionToolVisibilityRule.Layer.ALWAYS_ON;
import static fr.ailegalcase.casefile.DecisionToolVisibilityRule.Layer.CONTEXTUAL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DecisionToolVisibilityServiceTest {

    @Mock private DecisionToolVisibilityRuleRepository ruleRepository;
    @Mock private CaseFileRepository caseFileRepository;
    @Mock private CaseAnalysisRepository caseAnalysisRepository;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private CurrentUserResolver currentUserResolver;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private DecisionToolVisibilityService service;

    private static final UUID WS_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    private static final UUID CF_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");

    @BeforeEach
    void setUp() {
        service = new DecisionToolVisibilityService(
                ruleRepository, caseFileRepository, caseAnalysisRepository,
                workspaceMemberRepository, currentUserResolver, objectMapper);
    }

    // ────────────────────────────── Travail FR ──────────────────────────────

    @Test
    void travailFr_ruptureConventionnelle_contextualContains_FDT10_F132() {
        givenTravailFrRules();
        mockCaseFile("DROIT_DU_TRAVAIL", "FRANCE");
        mockLatestAnalysisJson("""
                { "compensation_data": { "type_rupture": "RUPTURE_CONVENTIONNELLE" } }
                """);

        VisibleToolSetResponse r = service.resolveVisibleTools(CF_ID, null, null);

        assertThat(r.alwaysOn()).containsExactly(
                "F-DT-03-prescription-litige",
                "F-DT-04-fiche-prudhomale",
                "F-DT-07-anciennete-conges-prime",
                "F-DT-01-calcul-indemnite-simple");
        assertThat(r.contextual()).containsExactly(
                "F-DT-10-rupture-conv-validity",
                "F-132-rupture-conv-indemnite");
        assertThat(r.catalog()).containsExactly(
                "F-DT-08-licenciement-validity",
                "F-DT-09-comparateur-indemnites");
    }

    @Test
    void travailFr_licenciement_contextualContains_FDT08_FDT09() {
        givenTravailFrRules();
        mockCaseFile("DROIT_DU_TRAVAIL", "FRANCE");
        mockLatestAnalysisJson("""
                { "compensation_data": { "type_rupture": "LICENCIEMENT" } }
                """);

        VisibleToolSetResponse r = service.resolveVisibleTools(CF_ID, null, null);

        assertThat(r.contextual()).containsExactly(
                "F-DT-08-licenciement-validity",
                "F-DT-09-comparateur-indemnites");
        assertThat(r.catalog()).containsExactly(
                "F-132-rupture-conv-indemnite",
                "F-DT-10-rupture-conv-validity");
    }

    @Test
    void travailFr_licenciementEconomique_alsoActivates_FDT08_FDT09() {
        givenTravailFrRules();
        mockCaseFile("DROIT_DU_TRAVAIL", "FRANCE");
        mockLatestAnalysisJson("""
                { "compensation_data": { "type_rupture": "LICENCIEMENT_ECONOMIQUE" } }
                """);

        VisibleToolSetResponse r = service.resolveVisibleTools(CF_ID, null, null);

        assertThat(r.contextual()).containsExactlyInAnyOrder(
                "F-DT-08-licenciement-validity",
                "F-DT-09-comparateur-indemnites");
    }

    // ────────────────────────────── Travail BE ──────────────────────────────

    @Test
    void travailBe_licenciementOrdinaire_activatesBEtools_notFRtools() {
        givenTravailBeRules();
        mockCaseFile("DROIT_DU_TRAVAIL", "BELGIQUE");
        mockLatestAnalysisJson("""
                { "compensation_data": { "type_rupture": "LICENCIEMENT_ORDINAIRE" } }
                """);

        VisibleToolSetResponse r = service.resolveVisibleTools(CF_ID, null, null);

        assertThat(r.alwaysOn()).containsExactly(
                "F-DT-05-preavis-be",
                "F-DT-06-requete-tribunal-travail",
                "F-DT-07-anciennete-conges-prime");
        assertThat(r.contextual()).containsExactlyInAnyOrder(
                "F-DT-08-licenciement-validity",
                "F-DT-09-comparateur-indemnites");
    }

    // ──────────────────────────── Pas d'analyse ─────────────────────────────

    @Test
    void immigrationFr_sansAnalyse_alwaysOnFilled_contextualEmpty_catalogFull() {
        givenImmigrationTransversalRules();
        mockCaseFile("DROIT_IMMIGRATION", "FRANCE");
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                eq(CF_ID), eq(AnalysisStatus.DONE))).thenReturn(Optional.empty());

        VisibleToolSetResponse r = service.resolveVisibleTools(CF_ID, null, null);

        assertThat(r.alwaysOn()).containsExactly(
                "F-IM-05-arbre-decisionnel-titre",
                "F-IM-07-droit-au-travail");
        assertThat(r.contextual()).isEmpty();
        assertThat(r.catalog()).contains("F-IM-01-checklist-pieces", "F-IM-06-recours");
    }

    // ─────────────────────────── Multi-détection ────────────────────────────

    @Test
    void famille_regimeMatrimonial_plus_procedureDetectee_multiSituations() {
        givenFamilleRules();
        mockCaseFile("DROIT_FAMILLE", "FRANCE");
        mockLatestAnalysisJson("""
                {
                  "liquidation_communaute_data": { "regime_matrimonial": "COMMUNAUTE_LEGALE" },
                  "type_procedure_detectee": "DIVORCE_CONSENTEMENT_MUTUEL"
                }
                """);

        VisibleToolSetResponse r = service.resolveVisibleTools(CF_ID, null, null);

        assertThat(r.contextual()).contains(
                "F-FA-05-partage-immobilier",
                "F-FA-07-checklist-divorce",
                "F-152-divorce-consentement-scoring");
    }

    // ─────────────────────── Fallback workspace country null ────────────────

    @Test
    void workspaceCountryNull_chargeSeulementLesReglesTransversales() {
        List<DecisionToolVisibilityRule> transverseOnly = List.of(
                rule("DROIT_IMMIGRATION", null, "F-IM-05-arbre-decisionnel-titre", ALWAYS_ON, null, null, 10));
        when(ruleRepository.findForDomainAndCountry("DROIT_IMMIGRATION", null))
                .thenReturn(transverseOnly);
        mockCaseFile("DROIT_IMMIGRATION", null);
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(any(), any()))
                .thenReturn(Optional.empty());

        VisibleToolSetResponse r = service.resolveVisibleTools(CF_ID, null, null);

        assertThat(r.alwaysOn()).containsExactly("F-IM-05-arbre-decisionnel-titre");
    }

    // ──────────────────────── Isolation workspace ──────────────────────────

    @Test
    void caseFileOtherWorkspace_returns404() {
        User user = new User();
        user.setId(UUID.randomUUID());
        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);

        CaseFile cf = buildCaseFile("DROIT_DU_TRAVAIL", "FRANCE");
        when(caseFileRepository.findByIdAndDeletedAtIsNull(CF_ID)).thenReturn(Optional.of(cf));

        Workspace otherWs = new Workspace();
        otherWs.setId(UUID.fromString("99999999-0000-0000-0000-000000000099"));
        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(otherWs);
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));

        assertThatThrownBy(() -> service.resolveVisibleTools(CF_ID, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Case file not found");
    }

    @Test
    void caseFileNotFound_returns404() {
        User user = new User();
        user.setId(UUID.randomUUID());
        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);
        when(caseFileRepository.findByIdAndDeletedAtIsNull(CF_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveVisibleTools(CF_ID, null, null))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("Case file not found");
    }

    // ─────────────────────────── JSON invalide ──────────────────────────────

    @Test
    void invalidJson_isHandledGracefully_alwaysOnAndCatalogStillFilled() {
        givenTravailFrRules();
        mockCaseFile("DROIT_DU_TRAVAIL", "FRANCE");
        CaseAnalysis analysis = new CaseAnalysis();
        analysis.setId(UUID.randomUUID());
        analysis.setAnalysisResult("not a json {{{");
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                eq(CF_ID), eq(AnalysisStatus.DONE))).thenReturn(Optional.of(analysis));

        VisibleToolSetResponse r = service.resolveVisibleTools(CF_ID, null, null);

        assertThat(r.alwaysOn()).isNotEmpty();
        assertThat(r.contextual()).isEmpty();
        assertThat(r.catalog()).isNotEmpty();
    }

    // ─────── F-165 SF-165-01 : 5 nouveaux trigger_field Travail FR ──────────

    @Test
    void travailFr_typeContrat_CDD_active_F_DT_17_F_DT_22() {
        givenTravailFrRulesF165();
        mockCaseFile("DROIT_DU_TRAVAIL", "FRANCE");
        mockLatestAnalysisJson("{\"travail_extracted_data\":{\"type_contrat\":\"CDD\"}}");

        VisibleToolSetResponse r = service.resolveVisibleTools(CF_ID, null, null);

        assertThat(r.contextual())
                .contains("F-DT-17-indemnite-precarite-cdd")
                .contains("F-DT-22-requalification-cdd-cdi")
                .doesNotContain("F-DT-18-fin-mission-interim")
                .doesNotContain("F-DT-23-requalification-interim-cdi");
    }

    @Test
    void travailFr_typeContrat_INTERIM_active_F_DT_18_F_DT_23() {
        givenTravailFrRulesF165();
        mockCaseFile("DROIT_DU_TRAVAIL", "FRANCE");
        mockLatestAnalysisJson("{\"travail_extracted_data\":{\"type_contrat\":\"INTERIM\"}}");

        VisibleToolSetResponse r = service.resolveVisibleTools(CF_ID, null, null);

        assertThat(r.contextual())
                .contains("F-DT-18-fin-mission-interim")
                .contains("F-DT-23-requalification-interim-cdi")
                .doesNotContain("F-DT-17-indemnite-precarite-cdd");
    }

    @Test
    void travailFr_origine_inaptitude_active_F_DT_15() {
        givenTravailFrRulesF165();
        mockCaseFile("DROIT_DU_TRAVAIL", "FRANCE");
        mockLatestAnalysisJson("{\"travail_extracted_data\":{\"origine_inaptitude_pressentie\":\"MALADIE_PROFESSIONNELLE\"}}");

        VisibleToolSetResponse r = service.resolveVisibleTools(CF_ID, null, null);

        assertThat(r.contextual()).contains("F-DT-15-inaptitude");
    }

    @Test
    void travailFr_motif_nullite_HARCELEMENT_active_F_DT_11() {
        givenTravailFrRulesF165();
        mockCaseFile("DROIT_DU_TRAVAIL", "FRANCE");
        mockLatestAnalysisJson("{\"travail_extracted_data\":{\"motif_nullite_pressenti\":\"HARCELEMENT_MORAL\"}}");

        VisibleToolSetResponse r = service.resolveVisibleTools(CF_ID, null, null);

        assertThat(r.contextual())
                .contains("F-DT-11-harcelement-licenciement-nul")
                .doesNotContain("F-DT-12-discrimination-dommages-interets");
    }

    @Test
    void travailFr_heures_sup_objet_present_active_F_DT_19() {
        givenTravailFrRulesF165();
        mockCaseFile("DROIT_DU_TRAVAIL", "FRANCE");
        mockLatestAnalysisJson(
                "{\"travail_extracted_data\":{\"heures_sup_mentionnees\":{\"total_declarees_25pct\":12}}}");

        VisibleToolSetResponse r = service.resolveVisibleTools(CF_ID, null, null);

        assertThat(r.contextual()).contains("F-DT-19-heures-sup");
    }

    @Test
    void travailFr_heures_sup_node_null_pas_de_trigger() {
        givenTravailFrRulesF165();
        mockCaseFile("DROIT_DU_TRAVAIL", "FRANCE");
        mockLatestAnalysisJson("{\"travail_extracted_data\":{\"heures_sup_mentionnees\":null}}");

        VisibleToolSetResponse r = service.resolveVisibleTools(CF_ID, null, null);

        assertThat(r.contextual()).doesNotContain("F-DT-19-heures-sup");
    }

    @Test
    void travailFr_dossier_vide_aucun_contextuel_F165() {
        givenTravailFrRulesF165();
        mockCaseFile("DROIT_DU_TRAVAIL", "FRANCE");
        // Pas d'analyse → pas de detection → seulement les ALWAYS_ON
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                eq(CF_ID), eq(AnalysisStatus.DONE))).thenReturn(Optional.empty());

        VisibleToolSetResponse r = service.resolveVisibleTools(CF_ID, null, null);

        assertThat(r.contextual()).isEmpty();
        assertThat(r.alwaysOn())
                .contains("F-DT-03-prescription-litige")
                .contains("F-DT-04-fiche-prudhomale")
                .contains("F-DT-07-anciennete-conges-prime")
                .doesNotContain("F-DT-15-inaptitude")
                .doesNotContain("F-DT-17-indemnite-precarite-cdd");
    }

    /** Reproduit le seeding réel post F-165 SF-165-01 (migration 193). */
    private void givenTravailFrRulesF165() {
        List<DecisionToolVisibilityRule> rules = new ArrayList<>();
        // Essentiels ALWAYS_ON
        rules.add(rule("DROIT_DU_TRAVAIL", "FRANCE", "F-DT-03-prescription-litige", ALWAYS_ON, null, null, 10));
        rules.add(rule("DROIT_DU_TRAVAIL", "FRANCE", "F-DT-04-fiche-prudhomale", ALWAYS_ON, null, null, 20));
        rules.add(rule("DROIT_DU_TRAVAIL", "FRANCE", "F-DT-07-anciennete-conges-prime", ALWAYS_ON, null, null, 30));
        // F-165 SF-165-01 : nouvelles entrées CONTEXTUAL
        rules.add(rule("DROIT_DU_TRAVAIL", "FRANCE", "F-DT-17-indemnite-precarite-cdd", CONTEXTUAL, "type_contrat", "CDD", 50));
        rules.add(rule("DROIT_DU_TRAVAIL", "FRANCE", "F-DT-22-requalification-cdd-cdi", CONTEXTUAL, "type_contrat", "CDD", 54));
        rules.add(rule("DROIT_DU_TRAVAIL", "FRANCE", "F-DT-18-fin-mission-interim", CONTEXTUAL, "type_contrat", "INTERIM", 51));
        rules.add(rule("DROIT_DU_TRAVAIL", "FRANCE", "F-DT-23-requalification-interim-cdi", CONTEXTUAL, "type_contrat", "INTERIM", 58));
        rules.add(rule("DROIT_DU_TRAVAIL", "FRANCE", "F-DT-15-inaptitude", CONTEXTUAL, "origine_inaptitude_pressentie", "MALADIE_PROFESSIONNELLE", 54));
        rules.add(rule("DROIT_DU_TRAVAIL", "FRANCE", "F-DT-11-harcelement-licenciement-nul", CONTEXTUAL, "motif_nullite_pressenti", "HARCELEMENT_MORAL", 52));
        rules.add(rule("DROIT_DU_TRAVAIL", "FRANCE", "F-DT-12-discrimination-dommages-interets", CONTEXTUAL, "motif_nullite_pressenti", "DISCRIMINATION", 53));
        rules.add(rule("DROIT_DU_TRAVAIL", "FRANCE", "F-DT-19-heures-sup", CONTEXTUAL, "heures_sup_mentionnees", "PRESENT", 55));
        when(ruleRepository.findForDomainAndCountry("DROIT_DU_TRAVAIL", "FRANCE")).thenReturn(rules);
    }

    // ─────────────────────────── Helpers ────────────────────────────────────

    private void givenTravailFrRules() {
        List<DecisionToolVisibilityRule> rules = new ArrayList<>();
        rules.add(rule("DROIT_DU_TRAVAIL", "FRANCE", "F-DT-03-prescription-litige", ALWAYS_ON, null, null, 10));
        rules.add(rule("DROIT_DU_TRAVAIL", "FRANCE", "F-DT-04-fiche-prudhomale", ALWAYS_ON, null, null, 20));
        rules.add(rule("DROIT_DU_TRAVAIL", "FRANCE", "F-DT-07-anciennete-conges-prime", ALWAYS_ON, null, null, 30));
        rules.add(rule("DROIT_DU_TRAVAIL", "FRANCE", "F-DT-01-calcul-indemnite-simple", ALWAYS_ON, null, null, 40));
        rules.add(rule("DROIT_DU_TRAVAIL", "FRANCE", "F-DT-08-licenciement-validity", CONTEXTUAL, "type_rupture", "LICENCIEMENT", 10));
        rules.add(rule("DROIT_DU_TRAVAIL", "FRANCE", "F-DT-08-licenciement-validity", CONTEXTUAL, "type_rupture", "LICENCIEMENT_ECONOMIQUE", 10));
        rules.add(rule("DROIT_DU_TRAVAIL", "FRANCE", "F-DT-09-comparateur-indemnites", CONTEXTUAL, "type_rupture", "LICENCIEMENT", 20));
        rules.add(rule("DROIT_DU_TRAVAIL", "FRANCE", "F-DT-09-comparateur-indemnites", CONTEXTUAL, "type_rupture", "LICENCIEMENT_ECONOMIQUE", 20));
        rules.add(rule("DROIT_DU_TRAVAIL", "FRANCE", "F-DT-10-rupture-conv-validity", CONTEXTUAL, "type_rupture", "RUPTURE_CONVENTIONNELLE", 10));
        rules.add(rule("DROIT_DU_TRAVAIL", "FRANCE", "F-132-rupture-conv-indemnite", CONTEXTUAL, "type_rupture", "RUPTURE_CONVENTIONNELLE", 20));
        when(ruleRepository.findForDomainAndCountry("DROIT_DU_TRAVAIL", "FRANCE")).thenReturn(rules);
    }

    private void givenTravailBeRules() {
        List<DecisionToolVisibilityRule> rules = new ArrayList<>();
        rules.add(rule("DROIT_DU_TRAVAIL", "BELGIQUE", "F-DT-05-preavis-be", ALWAYS_ON, null, null, 10));
        rules.add(rule("DROIT_DU_TRAVAIL", "BELGIQUE", "F-DT-06-requete-tribunal-travail", ALWAYS_ON, null, null, 20));
        rules.add(rule("DROIT_DU_TRAVAIL", "BELGIQUE", "F-DT-07-anciennete-conges-prime", ALWAYS_ON, null, null, 30));
        rules.add(rule("DROIT_DU_TRAVAIL", "BELGIQUE", "F-DT-08-licenciement-validity", CONTEXTUAL, "type_rupture", "LICENCIEMENT_ORDINAIRE", 10));
        rules.add(rule("DROIT_DU_TRAVAIL", "BELGIQUE", "F-DT-09-comparateur-indemnites", CONTEXTUAL, "type_rupture", "LICENCIEMENT_ORDINAIRE", 20));
        when(ruleRepository.findForDomainAndCountry("DROIT_DU_TRAVAIL", "BELGIQUE")).thenReturn(rules);
    }

    private void givenImmigrationTransversalRules() {
        List<DecisionToolVisibilityRule> rules = new ArrayList<>();
        rules.add(rule("DROIT_IMMIGRATION", null, "F-IM-05-arbre-decisionnel-titre", ALWAYS_ON, null, null, 10));
        rules.add(rule("DROIT_IMMIGRATION", null, "F-IM-07-droit-au-travail", ALWAYS_ON, null, null, 20));
        rules.add(rule("DROIT_IMMIGRATION", null, "F-IM-01-checklist-pieces", CONTEXTUAL, "type_titre_sejour_code", "VLS_TS_SALARIE", 10));
        rules.add(rule("DROIT_IMMIGRATION", null, "F-IM-06-recours", CONTEXTUAL, "type_recours_code", "RECOURS_CONTENTIEUX_TA", 20));
        when(ruleRepository.findForDomainAndCountry("DROIT_IMMIGRATION", "FRANCE")).thenReturn(rules);
    }

    private void givenFamilleRules() {
        List<DecisionToolVisibilityRule> rules = new ArrayList<>();
        rules.add(rule("DROIT_FAMILLE", null, "F-FA-01-prestation-compensatoire", ALWAYS_ON, null, null, 10));
        rules.add(rule("DROIT_FAMILLE", null, "F-FA-05-partage-immobilier", CONTEXTUAL, "regime_matrimonial", "COMMUNAUTE_LEGALE", 10));
        rules.add(rule("DROIT_FAMILLE", null, "F-FA-07-checklist-divorce", CONTEXTUAL, "type_procedure_detectee", "DIVORCE_CONSENTEMENT_MUTUEL", 30));
        rules.add(rule("DROIT_FAMILLE", null, "F-152-divorce-consentement-scoring", CONTEXTUAL, "type_procedure_detectee", "DIVORCE_CONSENTEMENT_MUTUEL", 40));
        when(ruleRepository.findForDomainAndCountry("DROIT_FAMILLE", "FRANCE")).thenReturn(rules);
    }

    private static DecisionToolVisibilityRule rule(String domain, String country, String toolId,
                                                    DecisionToolVisibilityRule.Layer layer,
                                                    String triggerField, String triggerValue, int priority) {
        DecisionToolVisibilityRule r = new DecisionToolVisibilityRule();
        r.setLegalDomain(domain);
        r.setCountry(country);
        r.setToolId(toolId);
        r.setLayer(layer);
        r.setTriggerField(triggerField);
        r.setTriggerValue(triggerValue);
        r.setPriority(priority);
        return r;
    }

    private void mockCaseFile(String legalDomain, String country) {
        User user = new User();
        user.setId(UUID.randomUUID());
        when(currentUserResolver.resolve(any(), any(), any())).thenReturn(user);

        CaseFile cf = buildCaseFile(legalDomain, country);
        when(caseFileRepository.findByIdAndDeletedAtIsNull(CF_ID)).thenReturn(Optional.of(cf));

        WorkspaceMember member = new WorkspaceMember();
        member.setWorkspace(cf.getWorkspace());
        when(workspaceMemberRepository.findByUserAndPrimaryTrue(user)).thenReturn(Optional.of(member));
    }

    private CaseFile buildCaseFile(String legalDomain, String country) {
        Workspace ws = new Workspace();
        ws.setId(WS_ID);
        ws.setLegalDomain(legalDomain);
        ws.setCountry(country);

        CaseFile cf = new CaseFile();
        cf.setId(CF_ID);
        cf.setWorkspace(ws);
        cf.setLegalDomain(legalDomain);
        return cf;
    }

    private void mockLatestAnalysisJson(String json) {
        CaseAnalysis analysis = new CaseAnalysis();
        analysis.setId(UUID.randomUUID());
        analysis.setAnalysisResult(json);
        when(caseAnalysisRepository.findFirstByCaseFileIdAndAnalysisStatusOrderByUpdatedAtDesc(
                eq(CF_ID), eq(AnalysisStatus.DONE))).thenReturn(Optional.of(analysis));
    }
}
