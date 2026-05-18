package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-06 — tests de la cellule CPH / DEPARTAGE / DEFENDEUR : combinaison
 * déclarée et prompt système (formation de départage, rôle défendeur / employeur,
 * dispositif « débouter »).
 */
class CphDepartageDefendeurPromptProviderTest {

    private final CphDepartageDefendeurPromptProvider provider = new CphDepartageDefendeurPromptProvider();

    @Test
    void combination_isWorkTribunalFranceDepartageDefendant() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_DU_TRAVAIL);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.FRANCE);
        assertThat(key.jurisdiction()).isEqualTo("CPH");
        assertThat(key.stage()).isEqualTo("DEPARTAGE");
        assertThat(key.position()).isEqualTo("DEFENDEUR");
    }

    @Test
    void systemPrompt_targetsDepartageDefendantEmployerRoleAndDismissalRelief() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("avocat du défendeur (employeur)");
        assertThat(prompt).contains("Conseil de prud'hommes");
        assertThat(prompt).contains("formation de départage");
        assertThat(prompt).contains("juge départiteur");
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
