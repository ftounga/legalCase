package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-16 — tests de la cellule CT / APPEL / INTIME (Belgique) : combinaison
 * déclarée et prompt système (rôle intimé, cour du travail belge, confirmation du
 * jugement entrepris, appel incident).
 */
class CtAppelIntimePromptProviderTest {

    private final CtAppelIntimePromptProvider provider = new CtAppelIntimePromptProvider();

    @Test
    void combination_isLabourCourtBelgiumAppealRespondent() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_DU_TRAVAIL);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.BELGIQUE);
        assertThat(key.jurisdiction()).isEqualTo("CT");
        assertThat(key.stage()).isEqualTo("APPEL");
        assertThat(key.position()).isEqualTo("INTIME");
    }

    @Test
    void systemPrompt_targetsBelgianLabourCourtRespondentAppeal() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("cour du travail");
        assertThat(prompt).contains("intimé");
        assertThat(prompt).contains("Code judiciaire");
        assertThat(prompt).contains("CONFIRMATION DU JUGEMENT ENTREPRIS");
        assertThat(prompt).contains("APPEL INCIDENT");
        assertThat(prompt).contains("PAR CES MOTIFS");
    }

    @Test
    void systemPrompt_hasNoFrenchLawReference() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).doesNotContain("954");
        assertThat(prompt).doesNotContain("code de procédure civile");
        assertThat(prompt).doesNotContain("Conseil de prud'hommes");
    }

    @Test
    void systemPrompt_keepsTransverseDraftingInstructions() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("Pièce n° X");
        assertThat(prompt).contains("outils décisionnels");
        assertThat(prompt).contains("relu par l'avocat");
    }
}
