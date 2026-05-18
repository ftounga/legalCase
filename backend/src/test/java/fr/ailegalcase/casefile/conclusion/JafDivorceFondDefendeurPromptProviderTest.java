package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-31 — tests de la cellule JAF / DIVORCE_FOND / DEFENDEUR :
 * combinaison déclarée et prompt système (rôle défendeur, défense au divorce
 * contentieux, demande reconventionnelle).
 */
class JafDivorceFondDefendeurPromptProviderTest {

    private final JafDivorceFondDefendeurPromptProvider provider =
            new JafDivorceFondDefendeurPromptProvider();

    @Test
    void combination_isFamilyLawFranceJafDivorceFondDefendant() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_FAMILLE);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.FRANCE);
        assertThat(key.jurisdiction()).isEqualTo("JAF");
        assertThat(key.stage()).isEqualTo("DIVORCE_FOND");
        assertThat(key.position()).isEqualTo("DEFENDEUR");
    }

    @Test
    void systemPrompt_targetsDefendantRoleAndDivorceDefenceBeforeJaf() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("avocat du défendeur");
        assertThat(prompt).contains("juge aux affaires familiales (JAF)");
        assertThat(prompt).contains("divorce contentieux");
        assertThat(prompt).contains("CONCLUSIONS EN DÉFENSE");
        assertThat(prompt).contains("demande reconventionnelle");
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
