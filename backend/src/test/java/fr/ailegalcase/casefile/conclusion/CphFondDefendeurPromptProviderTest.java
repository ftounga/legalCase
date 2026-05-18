package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-02 — tests de la cellule CPH / FOND / DEFENDEUR : combinaison
 * déclarée et prompt système (rôle défendeur / employeur, dispositif « débouter »).
 */
class CphFondDefendeurPromptProviderTest {

    private final CphFondDefendeurPromptProvider provider = new CphFondDefendeurPromptProvider();

    @Test
    void combination_isWorkTribunalFranceFondDefendant() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_DU_TRAVAIL);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.FRANCE);
        assertThat(key.jurisdiction()).isEqualTo("CPH");
        assertThat(key.stage()).isEqualTo("FOND");
        assertThat(key.position()).isEqualTo("DEFENDEUR");
    }

    @Test
    void systemPrompt_targetsDefendantEmployerRoleAndDismissalRelief() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("avocat du défendeur (employeur)");
        assertThat(prompt).contains("Conseil de prud'hommes");
        assertThat(prompt).contains("CONCLUSIONS EN DÉFENSE");
        assertThat(prompt).contains("débouter");
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
