package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-24 — tests de la cellule PREF / DEMANDE_TITRE / DEMANDEUR_TITRE :
 * combinaison déclarée et prompt système (mémoire de soutien préfecture, CESEDA,
 * démarche hors contentieux / demande gracieuse).
 */
class PrefDemandeTitrePromptProviderTest {

    private final PrefDemandeTitrePromptProvider provider = new PrefDemandeTitrePromptProvider();

    @Test
    void combination_isImmigrationFrancePrefectureTitleClaimant() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_IMMIGRATION);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.FRANCE);
        assertThat(key.jurisdiction()).isEqualTo("PREF");
        assertThat(key.stage()).isEqualTo("DEMANDE_TITRE");
        assertThat(key.position()).isEqualTo("DEMANDEUR_TITRE");
    }

    @Test
    void systemPrompt_targetsPrefectureTitleRequestOutsideLitigation() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("préfecture");
        assertThat(prompt).contains("titre de séjour");
        assertThat(prompt).contains("CESEDA");
        assertThat(prompt).contains("HORS CONTENTIEUX");
        assertThat(prompt).contains("demande gracieuse");
        assertThat(prompt).contains("MÉMOIRE DE SOUTIEN");
    }
}
