package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-43 — tests de la cellule TF / REFERE / DEFENDEUR (Belgique) :
 * combinaison déclarée et prompt système ancré dans la procédure belge des
 * affaires réputées urgentes (Code judiciaire art. 1253ter/4), posture défendeur.
 */
class TfRefereDefendeurPromptProviderTest {

    private final TfRefereDefendeurPromptProvider provider = new TfRefereDefendeurPromptProvider();

    @Test
    void combination_isFamilyTribunalBelgiumRefereDefendant() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_FAMILLE);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.BELGIQUE);
        assertThat(key.jurisdiction()).isEqualTo("TF");
        assertThat(key.stage()).isEqualTo("REFERE");
        assertThat(key.position()).isEqualTo("DEFENDEUR");
    }

    @Test
    void systemPrompt_targetsBelgianFamilyTribunalAndDefendantRole() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("avocat du défendeur devant le tribunal de la famille");
        assertThat(prompt).contains("affaires réputées urgentes");
        assertThat(prompt).contains("Code judiciaire");
        assertThat(prompt).contains("art. 1253ter/4");
        assertThat(prompt).contains("Code civil");
        assertThat(prompt).contains("mesures débattues sont PROVISOIRES");
        assertThat(prompt).contains("PAR CES MOTIFS, plaise au Tribunal de la famille");
    }

    @Test
    void systemPrompt_hasDefendantPosture() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("POSTURE DÉFENDEUR");
        assertThat(prompt).contains("réfute le caractère urgent");
        assertThat(prompt).contains("demandes reconventionnelles provisoires");
    }

    @Test
    void systemPrompt_preservesF98Invariants() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("Cite les pièces par leur numéro (Pièce n° X).");
        assertThat(prompt).contains("Reprends les montants exacts des calculs fournis");
        assertThat(prompt).contains("Ce n'est qu'un projet");
    }

    @Test
    void systemPrompt_hasNoFrenchLawReference() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).doesNotContain("juge aux affaires familiales");
        assertThat(prompt).doesNotContain("ordonnance de non-conciliation");
    }
}
