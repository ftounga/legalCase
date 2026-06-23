package fr.ailegalcase.casefile.strategy;

import fr.ailegalcase.casefile.DashboardTile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-286 / SF-286-01 — tests du prompt de la stratégie de dossier. Vérifie que le prompt
 * consomme bien les verdicts CALCULÉS et la synthèse, et qu'il demande le format attendu
 * sans jamais demander au modèle de muter un outil.
 */
class CaseStrategyPromptBuilderTest {

    private final CaseStrategyPromptBuilder builder = new CaseStrategyPromptBuilder();

    private DashboardTile tile(String label, String primary, String secondary, String alert) {
        return new DashboardTile("tool-id", "VALIDITE", label, primary, secondary, alert);
    }

    @Test
    void systemPrompt_imposesSections_andForbidsRecompute() {
        String sys = CaseStrategyPromptBuilder.SYSTEM_PROMPT;
        assertThat(sys)
                .contains("## Voie procédurale")
                .contains("## Posture")
                .contains("## Priorisation des chefs de demande")
                .contains("## Séquencement")
                .contains("Tu ne RECALCULES rien")
                .contains("n'inventes AUCUN verdict");
    }

    @Test
    void systemPrompt_forbidsFabricatingFactsOrEvidence() {
        // SF-291-03 — le garde anti-invention couvre aussi FAITS / PREUVES / PIÈCES,
        // pas seulement verdicts/montants/délais (cas observé : pièce justificative inventée).
        String sys = CaseStrategyPromptBuilder.SYSTEM_PROMPT;
        assertThat(sys)
                .contains("FAIT")
                .contains("PREUVE")
                .contains("PIÈCE")
                .contains("établi par le dossier");
    }

    @Test
    void userMessage_includesCalculatedVerdicts() {
        String msg = builder.buildUserMessage(
                "Dossier Durand",
                List.of(tile("Validité du licenciement", "Sans cause réelle et sérieuse",
                        "2 critères sur 7", "ALERT")),
                "{\"faits\":[\"licenciement\"]}",
                List.of("Contester la cause réelle et sérieuse [Base juridique : L.1235-3]"));

        assertThat(msg)
                .contains("Dossier Durand")
                .contains("Validité du licenciement : Sans cause réelle et sérieuse")
                .contains("2 critères sur 7")
                .contains("[ALERT]")
                .contains("Contester la cause réelle et sérieuse")
                .contains("{\"faits\":[\"licenciement\"]}");
    }

    @Test
    void userMessage_noTiles_statesNoneCalculated() {
        String msg = builder.buildUserMessage("Dossier vide", List.of(), null, List.of());
        assertThat(msg)
                .contains("Aucun outil décisionnel calculé.")
                .contains("Synthèse indisponible.")
                .contains("Aucune piste stratégique retenue.");
    }
}
