package fr.ailegalcase.analysis;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.ailegalcase.immigration.ImmigrationStrategyScenario;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-151 SF-151-01 : tests du parseur {@code extractImmigrationStrategyScenarios}.
 */
class ImmigrationStrategyScenarioParsingTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static JsonNode json(String raw) throws Exception {
        return MAPPER.readTree(raw);
    }

    @Test
    void U01_parseDeuxScenarios_complets() throws Exception {
        JsonNode root = json("""
                {
                  "strategy_scenarios": [
                    {
                      "scenario_label": "Changement de statut immédiat",
                      "scenario_description": "Déposer maintenant une demande VPF au titre du mariage.",
                      "base_legale": "Art. L.423-1 CESEDA",
                      "target_title_code": "CST_VPF",
                      "target_title_label": "Carte Vie privée et familiale",
                      "delay_days_estimate": "90-180",
                      "risk_level": "FAIBLE",
                      "risk_justification": "Conditions remplies.",
                      "required_additional_pieces": ["Justificatif vie commune"],
                      "advantages": ["Droit au travail plein"],
                      "drawbacks": ["Perte mention Recherche"]
                    },
                    {
                      "scenario_label": "Attendre expiration",
                      "scenario_description": "Conserver le statut étudiant jusqu'à la soutenance de thèse.",
                      "base_legale": "Art. L.422-3 CESEDA",
                      "target_title_code": null,
                      "target_title_label": "Renouvellement carte pluriannuelle étudiant",
                      "delay_days_estimate": "60-120",
                      "risk_level": "MOYEN",
                      "risk_justification": "Dépend du calendrier thèse.",
                      "required_additional_pieces": [],
                      "advantages": ["Conservation mention Recherche"],
                      "drawbacks": ["Pas de droit au travail plein", "Délai contraint"]
                    }
                  ]
                }
                """);
        List<ImmigrationStrategyScenario> scenarios = CaseAnalysisResponse.extractImmigrationStrategyScenarios(root);

        assertThat(scenarios).hasSize(2);
        ImmigrationStrategyScenario s1 = scenarios.get(0);
        assertThat(s1.scenarioLabel()).isEqualTo("Changement de statut immédiat");
        assertThat(s1.baseLegale()).contains("L.423-1");
        assertThat(s1.riskLevel()).isEqualTo("FAIBLE");
        assertThat(s1.requiredAdditionalPieces()).containsExactly("Justificatif vie commune");
        assertThat(s1.advantages()).containsExactly("Droit au travail plein");
        assertThat(s1.drawbacks()).containsExactly("Perte mention Recherche");

        ImmigrationStrategyScenario s2 = scenarios.get(1);
        assertThat(s2.riskLevel()).isEqualTo("MOYEN");
        assertThat(s2.targetTitleCode()).isNull();
        assertThat(s2.drawbacks()).hasSize(2);
    }

    @Test
    void U02_tableauAbsent_listeVide() throws Exception {
        JsonNode root = json("""
                { "faits": [] }
                """);
        assertThat(CaseAnalysisResponse.extractImmigrationStrategyScenarios(root)).isEmpty();
    }

    @Test
    void U03_riskLevelInvalide_champNullSansSkipperLeScenario() throws Exception {
        JsonNode root = json("""
                {
                  "strategy_scenarios": [{
                    "scenario_label": "Scenario X",
                    "scenario_description": "Description X",
                    "risk_level": "TRES_ELEVE"
                  }]
                }
                """);
        List<ImmigrationStrategyScenario> scenarios = CaseAnalysisResponse.extractImmigrationStrategyScenarios(root);
        assertThat(scenarios).hasSize(1);
        assertThat(scenarios.get(0).riskLevel()).isNull();
    }

    @Test
    void U04_scenarioSansLabelOuDescription_skip() throws Exception {
        JsonNode root = json("""
                {
                  "strategy_scenarios": [
                    {"scenario_description": "pas de label"},
                    {"scenario_label": "pas de description"},
                    {"scenario_label": "OK", "scenario_description": "OK aussi"}
                  ]
                }
                """);
        List<ImmigrationStrategyScenario> scenarios = CaseAnalysisResponse.extractImmigrationStrategyScenarios(root);
        assertThat(scenarios).hasSize(1);
        assertThat(scenarios.get(0).scenarioLabel()).isEqualTo("OK");
    }

    @Test
    void U05_listesOptionnelles_videsParDefaut() throws Exception {
        JsonNode root = json("""
                {
                  "strategy_scenarios": [{
                    "scenario_label": "Minimal",
                    "scenario_description": "Scenario minimal"
                  }]
                }
                """);
        List<ImmigrationStrategyScenario> scenarios = CaseAnalysisResponse.extractImmigrationStrategyScenarios(root);
        assertThat(scenarios).hasSize(1);
        assertThat(scenarios.get(0).requiredAdditionalPieces()).isEmpty();
        assertThat(scenarios.get(0).advantages()).isEmpty();
        assertThat(scenarios.get(0).drawbacks()).isEmpty();
    }
}
