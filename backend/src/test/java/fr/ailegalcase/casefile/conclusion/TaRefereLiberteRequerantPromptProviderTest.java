package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-19 — tests de la cellule TA / REFERE_LIBERTE / REQUERANT :
 * combinaison déclarée et prompt système (référé-liberté, art. L.521-2 CJA).
 */
class TaRefereLiberteRequerantPromptProviderTest {

    private final TaRefereLiberteRequerantPromptProvider provider =
            new TaRefereLiberteRequerantPromptProvider();

    @Test
    void combination_isImmigrationFranceTaRefereLiberteClaimant() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_IMMIGRATION);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.FRANCE);
        assertThat(key.jurisdiction()).isEqualTo("TA");
        assertThat(key.stage()).isEqualTo("REFERE_LIBERTE");
        assertThat(key.position()).isEqualTo("REQUERANT");
    }

    @Test
    void systemPrompt_targetsRefereLiberteAndItsLegalStandard() {
        // Le bloc texte """ retourne à la ligne au milieu de certaines phrases :
        // on normalise les espaces avant de chercher les marqueurs juridiques.
        String prompt = provider.systemPrompt().replaceAll("\\s+", " ");

        assertThat(prompt).contains("RÉFÉRÉ-LIBERTÉ");
        assertThat(prompt).contains("L.521-2 du code de justice administrative");
        assertThat(prompt).contains("liberté fondamentale");
        assertThat(prompt).contains("atteinte grave et manifestement illégale");
        assertThat(prompt).contains("URGENCE");
        assertThat(prompt).contains("PAR CES MOTIFS");
    }
}
