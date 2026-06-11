package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-63 — tests de la cellule CPH / BCO / DEFENDEUR : combinaison déclarée
 * et prompt système (rôle défendeur / employeur, conclusions en défense au stade
 * conciliation, frontière avec l'outil F-DT-84).
 */
class CphBcoDefendeurPromptProviderTest {

    private final CphBcoDefendeurPromptProvider provider = new CphBcoDefendeurPromptProvider();

    @Test
    void combination_isWorkTribunalFranceBcoDefendant() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_DU_TRAVAIL);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.FRANCE);
        assertThat(key.jurisdiction()).isEqualTo("CPH");
        assertThat(key.stage()).isEqualTo("BCO");
        assertThat(key.position()).isEqualTo("DEFENDEUR");
    }

    @Test
    void systemPrompt_targetsBcoDefendantDefenceConclusions() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("avocat du défendeur (employeur)");
        assertThat(prompt).contains("bureau de conciliation et d'orientation (BCO)");
        assertThat(prompt).contains("CONCLUSIONS EN DÉFENSE");
        assertThat(prompt).contains("article 700 du CPC");
        assertThat(prompt).contains("PAR CES MOTIFS");
    }

    @Test
    void systemPrompt_doesNotDuplicateConciliationStrategyTool() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("produis l'acte écrit");
        assertThat(prompt).doesNotContain("barème Macron");
    }

    @Test
    void systemPrompt_keepsTransverseDraftingInstructions() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("Pièce n° X");
        assertThat(prompt).contains("outils décisionnels");
        assertThat(prompt).contains("relu par l'avocat");
    }
}
