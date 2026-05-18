package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-14 — tests de la cellule TT / REFERE / DEFENDEUR (Belgique) :
 * combinaison déclarée et prompt système (rôle défendeur au référé devant le
 * président du tribunal du travail, contestation de l'urgence, ancrage belge).
 */
class TtRefereDefendeurPromptProviderTest {

    private final TtRefereDefendeurPromptProvider provider = new TtRefereDefendeurPromptProvider();

    @Test
    void combination_isWorkTribunalBelgiumRefereDefendant() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_DU_TRAVAIL);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.BELGIQUE);
        assertThat(key.jurisdiction()).isEqualTo("TT");
        assertThat(key.stage()).isEqualTo("REFERE");
        assertThat(key.position()).isEqualTo("DEFENDEUR");
    }

    @Test
    void systemPrompt_targetsDefendantRefereRoleAndUrgencyContestation() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("avocat du défendeur");
        assertThat(prompt).contains("référé");
        assertThat(prompt).contains("président du tribunal du travail");
        assertThat(prompt).contains("CONCLUSIONS EN DÉFENSE");
        assertThat(prompt).contains("contestation de l'urgence");
        assertThat(prompt).contains("article 584 du Code judiciaire");
        assertThat(prompt).contains("PAR CES MOTIFS");
        assertThat(prompt).contains("débouter");
    }

    @Test
    void systemPrompt_isBelgianAnchoredWithoutFrenchLaw() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("Belgique");
        assertThat(prompt).doesNotContain("Conseil de prud'hommes");
        assertThat(prompt).doesNotContain("Code du travail");
    }

    @Test
    void systemPrompt_keepsTransverseDraftingInstructions() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("Pièce n° X");
        assertThat(prompt).contains("outils décisionnels");
        assertThat(prompt).contains("relu par l'avocat");
    }
}
