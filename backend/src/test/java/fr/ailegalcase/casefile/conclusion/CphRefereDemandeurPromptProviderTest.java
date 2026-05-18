package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-03 — tests de la cellule CPH / REFERE / DEMANDEUR : combinaison
 * déclarée et prompt système (rôle demandeur / salarié, fondements du référé
 * prud'homal — contestation sérieuse, trouble manifestement illicite, provision).
 */
class CphRefereDemandeurPromptProviderTest {

    private final CphRefereDemandeurPromptProvider provider = new CphRefereDemandeurPromptProvider();

    @Test
    void combination_isWorkTribunalFranceRefereClaimant() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_DU_TRAVAIL);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.FRANCE);
        assertThat(key.jurisdiction()).isEqualTo("CPH");
        assertThat(key.stage()).isEqualTo("REFERE");
        assertThat(key.position()).isEqualTo("DEMANDEUR");
    }

    @Test
    void systemPrompt_targetsRefereClaimantRoleAndKeyGrounds() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("avocat du demandeur (salarié)");
        assertThat(prompt).contains("formation de référé du Conseil de prud'hommes");
        assertThat(prompt).contains("CONCLUSIONS DE RÉFÉRÉ");
        assertThat(prompt).contains("contestation sérieuse");
        assertThat(prompt).contains("trouble manifestement illicite");
        assertThat(prompt).contains("provision");
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
