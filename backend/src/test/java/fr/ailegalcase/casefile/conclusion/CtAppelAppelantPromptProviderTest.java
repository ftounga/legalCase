package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-15 — tests de la cellule CT / APPEL / APPELANT (Belgique) :
 * combinaison déclarée et prompt système (rôle appelant, appel belge devant la
 * cour du travail, critique du jugement entrepris et réformation).
 */
class CtAppelAppelantPromptProviderTest {

    private final CtAppelAppelantPromptProvider provider = new CtAppelAppelantPromptProvider();

    @Test
    void combination_isWorkCourtBelgiumAppealAppellant() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_DU_TRAVAIL);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.BELGIQUE);
        assertThat(key.jurisdiction()).isEqualTo("CT");
        assertThat(key.stage()).isEqualTo("APPEL");
        assertThat(key.position()).isEqualTo("APPELANT");
    }

    @Test
    void systemPrompt_targetsBelgianAppealBeforeWorkCourt() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("avocat de l'appelant");
        assertThat(prompt).contains("cour du travail");
        assertThat(prompt).contains("Belgique");
        assertThat(prompt).contains("Code judiciaire");
        assertThat(prompt).contains("jugement entrepris");
        assertThat(prompt).contains("réformer");
        assertThat(prompt).contains("PAR CES MOTIFS");
    }

    @Test
    void systemPrompt_hasNoFrenchLawReference() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).doesNotContain("954");
        assertThat(prompt).doesNotContain("Conseil de prud'hommes");
        assertThat(prompt).doesNotContain("chambre sociale");
    }
}
