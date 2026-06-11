package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-62 — tests de la cellule CPH / BCO / DEMANDEUR : combinaison déclarée
 * et prompt système (rôle demandeur / salarié, requête de saisine valant conclusions,
 * volet provisions, frontière avec l'outil F-DT-84).
 */
class CphBcoDemandeurPromptProviderTest {

    private final CphBcoDemandeurPromptProvider provider = new CphBcoDemandeurPromptProvider();

    @Test
    void combination_isWorkTribunalFranceBcoClaimant() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_DU_TRAVAIL);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.FRANCE);
        assertThat(key.jurisdiction()).isEqualTo("CPH");
        assertThat(key.stage()).isEqualTo("BCO");
        assertThat(key.position()).isEqualTo("DEMANDEUR");
    }

    @Test
    void systemPrompt_targetsBcoClaimantSaisineValantConclusions() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("avocat du demandeur (salarié)");
        assertThat(prompt).contains("bureau de conciliation et d'orientation (BCO)");
        assertThat(prompt).contains("REQUÊTE AUX FINS DE SAISINE DU CONSEIL DE PRUD'HOMMES VALANT CONCLUSIONS");
        assertThat(prompt).contains("provision");
        assertThat(prompt).contains("PAR CES MOTIFS");
    }

    @Test
    void systemPrompt_doesNotDuplicateConciliationStrategyTool() {
        String prompt = provider.systemPrompt();

        // Frontière avec F-DT-84 : la cellule produit l'acte écrit, pas l'analyse d'opportunité.
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
