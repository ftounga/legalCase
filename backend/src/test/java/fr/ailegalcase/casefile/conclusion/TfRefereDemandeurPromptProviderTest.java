package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-43 — tests de la cellule TF / REFERE / DEMANDEUR (Belgique) :
 * combinaison déclarée et prompt système ancré dans la procédure belge des
 * affaires réputées urgentes (Code judiciaire art. 1253ter/4).
 */
class TfRefereDemandeurPromptProviderTest {

    private final TfRefereDemandeurPromptProvider provider = new TfRefereDemandeurPromptProvider();

    @Test
    void combination_isFamilyTribunalBelgiumRefereClaimant() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_FAMILLE);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.BELGIQUE);
        assertThat(key.jurisdiction()).isEqualTo("TF");
        assertThat(key.stage()).isEqualTo("REFERE");
        assertThat(key.position()).isEqualTo("DEMANDEUR");
    }

    @Test
    void systemPrompt_targetsBelgianFamilyTribunalAndClaimantRole() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("avocat du demandeur devant le tribunal de la famille");
        assertThat(prompt).contains("affaires réputées urgentes");
        assertThat(prompt).contains("Code judiciaire");
        assertThat(prompt).contains("art. 1253ter/4");
        assertThat(prompt).contains("Code civil");
        assertThat(prompt).contains("mesures sollicitées sont PROVISOIRES");
        assertThat(prompt).contains("PAR CES MOTIFS, plaise au Tribunal de la famille");
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
