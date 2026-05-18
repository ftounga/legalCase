package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-34 — tests de la cellule JAF / REFERE / DEFENDEUR : combinaison
 * déclarée et prompt système (rôle défendeur, référé devant le juge aux affaires
 * familiales — contestation de l'urgence, contre-propositions sur les mesures
 * provisoires).
 */
class JafRefereDefendeurPromptProviderTest {

    private final JafRefereDefendeurPromptProvider provider = new JafRefereDefendeurPromptProvider();

    @Test
    void combination_isFamilyFranceJafRefereDefendant() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_FAMILLE);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.FRANCE);
        assertThat(key.jurisdiction()).isEqualTo("JAF");
        assertThat(key.stage()).isEqualTo("REFERE");
        assertThat(key.position()).isEqualTo("DEFENDEUR");
    }

    @Test
    void systemPrompt_targetsRefereDefendantRoleAndUrgencyContest() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("avocat du défendeur");
        assertThat(prompt).contains("juge aux affaires familiales statuant en référé");
        assertThat(prompt).contains("CONCLUSIONS DE RÉFÉRÉ EN DÉFENSE");
        assertThat(prompt).contains("contestation de l'urgence");
        assertThat(prompt).contains("mesures urgentes et provisoires");
        assertThat(prompt).contains("contre-propositions");
        assertThat(prompt).contains("débouter le demandeur");
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
