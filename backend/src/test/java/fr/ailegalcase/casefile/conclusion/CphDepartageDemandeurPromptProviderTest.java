package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-05 — tests de la cellule CPH / DEPARTAGE / DEMANDEUR : combinaison
 * déclarée et prompt système (rôle demandeur / salarié, audience tenue en
 * formation de départage devant le juge départiteur).
 */
class CphDepartageDemandeurPromptProviderTest {

    private final CphDepartageDemandeurPromptProvider provider = new CphDepartageDemandeurPromptProvider();

    @Test
    void combination_isWorkTribunalFranceDepartageDemandeur() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_DU_TRAVAIL);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.FRANCE);
        assertThat(key.jurisdiction()).isEqualTo("CPH");
        assertThat(key.stage()).isEqualTo("DEPARTAGE");
        assertThat(key.position()).isEqualTo("DEMANDEUR");
    }

    @Test
    void systemPrompt_targetsClaimantRoleAndDepartageHearing() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("avocat du demandeur (salarié)");
        assertThat(prompt).contains("Conseil de prud'hommes");
        assertThat(prompt).contains("formation de départage");
        assertThat(prompt).contains("juge départiteur");
        assertThat(prompt).contains("PAR CES MOTIFS");
    }

    @Test
    void systemPrompt_keepsTransverseDraftingInstructions() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("Pièce n° X");
        assertThat(prompt).contains("outils décisionnels");
        assertThat(prompt).contains("relu par l'avocat");
    }
}
