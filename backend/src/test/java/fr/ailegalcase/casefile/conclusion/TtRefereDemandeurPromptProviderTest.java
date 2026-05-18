package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-13 — tests de la cellule TT / REFERE / DEMANDEUR (Belgique) :
 * combinaison déclarée et prompt système (référé devant le président du tribunal
 * du travail belge, urgence, mesures provisoires, rôle demandeur).
 */
class TtRefereDemandeurPromptProviderTest {

    private final TtRefereDemandeurPromptProvider provider = new TtRefereDemandeurPromptProvider();

    @Test
    void combination_isWorkTribunalBelgiumRefereClaimant() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_DU_TRAVAIL);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.BELGIQUE);
        assertThat(key.jurisdiction()).isEqualTo("TT");
        assertThat(key.stage()).isEqualTo("REFERE");
        assertThat(key.position()).isEqualTo("DEMANDEUR");
    }

    @Test
    void systemPrompt_targetsBelgianRefereBeforeWorkTribunalPresident() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("avocat du demandeur");
        assertThat(prompt).contains("référé");
        assertThat(prompt).contains("président du tribunal du travail");
        assertThat(prompt).contains("article 584 du Code judiciaire");
        assertThat(prompt).contains("urgence");
        assertThat(prompt).contains("provisoire");
        assertThat(prompt).contains("PAR CES MOTIFS");
    }
}
