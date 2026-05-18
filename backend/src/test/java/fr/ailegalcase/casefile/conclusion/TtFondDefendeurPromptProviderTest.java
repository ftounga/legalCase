package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-12 — tests de la cellule TT / FOND / DEFENDEUR (Belgique) :
 * combinaison déclarée et prompt système (rôle défendeur / employeur, ancrage
 * belge, dispositif de défense « demandes non fondées »).
 */
class TtFondDefendeurPromptProviderTest {

    private final TtFondDefendeurPromptProvider provider = new TtFondDefendeurPromptProvider();

    @Test
    void combination_isWorkTribunalBelgiumFondDefendant() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_DU_TRAVAIL);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.BELGIQUE);
        assertThat(key.jurisdiction()).isEqualTo("TT");
        assertThat(key.stage()).isEqualTo("FOND");
        assertThat(key.position()).isEqualTo("DEFENDEUR");
    }

    @Test
    void systemPrompt_targetsDefendantEmployerRoleBeforeWorkTribunal() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("avocat du défendeur (employeur)");
        assertThat(prompt).contains("tribunal du travail");
        assertThat(prompt).contains("CONCLUSIONS EN DÉFENSE");
        assertThat(prompt).contains("non fondées");
        assertThat(prompt).contains("PAR CES MOTIFS");
    }

    @Test
    void systemPrompt_isAnchoredInBelgianProcedure() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("Code judiciaire");
        assertThat(prompt).contains("loi du 3 juillet 1978");
        assertThat(prompt).contains("CCT n° 109");
    }

    @Test
    void systemPrompt_containsNoFrenchLawReference() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).doesNotContain("prud'hommes");
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
