package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-34 — tests de la cellule JAF / REFERE / DEMANDEUR : combinaison
 * déclarée et prompt système (rôle demandeur, référé devant le juge aux affaires
 * familiales — urgence, mesures provisoires).
 */
class JafRefereDemandeurPromptProviderTest {

    private final JafRefereDemandeurPromptProvider provider = new JafRefereDemandeurPromptProvider();

    @Test
    void combination_isFamilyFranceJafRefereClaimant() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_FAMILLE);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.FRANCE);
        assertThat(key.jurisdiction()).isEqualTo("JAF");
        assertThat(key.stage()).isEqualTo("REFERE");
        assertThat(key.position()).isEqualTo("DEMANDEUR");
    }

    @Test
    void systemPrompt_targetsRefereClaimantRoleAndProvisionalMeasures() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("avocat du demandeur");
        assertThat(prompt).contains("juge aux affaires familiales statuant en référé");
        assertThat(prompt).contains("CONCLUSIONS DE RÉFÉRÉ");
        assertThat(prompt).contains("URGENCE");
        assertThat(prompt).contains("mesures urgentes et provisoires");
        assertThat(prompt).contains("autorité parentale");
        assertThat(prompt).contains("résidence des enfants");
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
