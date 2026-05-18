package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-42 — tests de la cellule TF / FOND / DEFENDEUR (Belgique) :
 * combinaison déclarée et prompt système ancré dans la procédure belge.
 */
class TfFondDefendeurPromptProviderTest {

    private final TfFondDefendeurPromptProvider provider = new TfFondDefendeurPromptProvider();

    @Test
    void combination_isFamilyTribunalBelgiumFondDefendant() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_FAMILLE);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.BELGIQUE);
        assertThat(key.jurisdiction()).isEqualTo("TF");
        assertThat(key.stage()).isEqualTo("FOND");
        assertThat(key.position()).isEqualTo("DEFENDEUR");
    }

    @Test
    void systemPrompt_targetsBelgianFamilyTribunalAndDefendantRole() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("avocat du défendeur");
        assertThat(prompt).contains("tribunal de la famille");
        assertThat(prompt).contains("Code judiciaire");
        assertThat(prompt).contains("Code civil belge");
        assertThat(prompt).contains("PROJET DE CONCLUSIONS");
        assertThat(prompt).contains("PAR CES MOTIFS, plaise au Tribunal de la famille");
    }

    @Test
    void systemPrompt_adoptsDefendantPosture() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("réfute chef par chef");
        assertThat(prompt).contains("demandes reconventionnelles");
        assertThat(prompt).contains("art. 572bis");
    }

    @Test
    void systemPrompt_keepsF98Invariants() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("Cite les pièces par leur numéro (Pièce n° X).");
        assertThat(prompt).contains("Reprends les montants exacts des calculs fournis");
        assertThat(prompt).contains("Ce n'est qu'un projet");
    }

    @Test
    void systemPrompt_hasNoFrenchLawReference() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).doesNotContain("juge aux affaires familiales");
        assertThat(prompt).doesNotContain("tribunal judiciaire");
    }
}
