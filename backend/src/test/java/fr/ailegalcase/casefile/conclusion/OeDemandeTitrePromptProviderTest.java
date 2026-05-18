package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-29 — tests de la cellule OE / DEMANDE_TITRE / DEMANDEUR_TITRE :
 * combinaison déclarée et marqueurs du prompt système (mémoire de demande de titre
 * belge adressé à l'Office des étrangers, ancrage loi du 15 décembre 1980, hors
 * contentieux, aucune référence au droit français).
 */
class OeDemandeTitrePromptProviderTest {

    private final OeDemandeTitrePromptProvider provider = new OeDemandeTitrePromptProvider();

    @Test
    void combination_isImmigrationBelgiumOfficeTitleClaimant() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_IMMIGRATION);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.BELGIQUE);
        assertThat(key.jurisdiction()).isEqualTo("OE");
        assertThat(key.stage()).isEqualTo("DEMANDE_TITRE");
        assertThat(key.position()).isEqualTo("DEMANDEUR_TITRE");
    }

    @Test
    void systemPrompt_targetsBelgianOfficeTitleRequest() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("Office des étrangers");
        assertThat(prompt).contains("MÉMOIRE DE SOUTIEN");
        assertThat(prompt).contains("DEMANDE D'AUTORISATION DE SÉJOUR");
        assertThat(prompt).contains("loi du 15 décembre 1980");
        assertThat(prompt).contains("art. 9bis");
        assertThat(prompt).contains("art. 9ter");
        assertThat(prompt).contains("HORS CONTENTIEUX");
    }

    @Test
    void systemPrompt_keepsCrossCuttingMatrixInstructions() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("Pièce n° X");
        assertThat(prompt).contains("verdicts des outils décisionnels");
        assertThat(prompt).contains("N'invente aucun chiffre");
        assertThat(prompt).contains("relu par l'avocat");
    }

    @Test
    void systemPrompt_hasNoFrenchLawReference() {
        String prompt = provider.systemPrompt().toLowerCase();

        assertThat(prompt).doesNotContain("préfecture");
        assertThat(prompt).doesNotContain("ceseda");
        assertThat(prompt).doesNotContain("droit français");
    }
}
