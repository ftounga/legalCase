package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-04 — tests de la cellule CPH / REFERE / DEFENDEUR : combinaison
 * déclarée et prompt système (rôle défendeur / employeur, défense au référé, moyen
 * central de la contestation sérieuse).
 */
class CphRefereDefendeurPromptProviderTest {

    private final CphRefereDefendeurPromptProvider provider = new CphRefereDefendeurPromptProvider();

    @Test
    void combination_isWorkTribunalFranceRefereDefendant() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_DU_TRAVAIL);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.FRANCE);
        assertThat(key.jurisdiction()).isEqualTo("CPH");
        assertThat(key.stage()).isEqualTo("REFERE");
        assertThat(key.position()).isEqualTo("DEFENDEUR");
    }

    @Test
    void systemPrompt_targetsRefereDefenceAndSeriousChallenge() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("avocat du défendeur (employeur)");
        assertThat(prompt).contains("formation de référé du Conseil de prud'hommes");
        assertThat(prompt).contains("CONCLUSIONS EN DÉFENSE AU RÉFÉRÉ");
        assertThat(prompt).contains("contestation sérieuse");
        assertThat(prompt).contains("trouble manifestement illicite");
        assertThat(prompt).contains("n'y avoir lieu à référé");
        assertThat(prompt).contains("PAR CES MOTIFS");
    }

    @Test
    void systemPrompt_keepsTransverseDraftingInstructions() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("Pièce n° X");
        assertThat(prompt).contains("outils décisionnels");
        assertThat(prompt).contains("n'invente aucun chiffre");
        assertThat(prompt).contains("relu par l'avocat");
    }
}
