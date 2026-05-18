package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-25 — tests de la cellule TA / RECOURS_TITRE / REQUERANT :
 * combinaison déclarée et prompt système (requête en annulation devant le
 * tribunal administratif contre un refus de titre / de regroupement familial).
 */
class TaRecoursTitreRequerantPromptProviderTest {

    private final TaRecoursTitreRequerantPromptProvider provider =
            new TaRecoursTitreRequerantPromptProvider();

    @Test
    void combination_isImmigrationFranceTaRecoursTitreClaimant() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_IMMIGRATION);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.FRANCE);
        assertThat(key.jurisdiction()).isEqualTo("TA");
        assertThat(key.stage()).isEqualTo("RECOURS_TITRE");
        assertThat(key.position()).isEqualTo("REQUERANT");
    }

    @Test
    void systemPrompt_targetsAdministrativeAnnulmentAgainstResidencePermitRefusal() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("tribunal administratif");
        assertThat(prompt).contains("REQUÊTE EN ANNULATION");
        assertThat(prompt).contains("refus de titre de séjour");
        assertThat(prompt).contains("refus de regroupement familial");
        assertThat(prompt).contains("LÉGALITÉ EXTERNE");
        assertThat(prompt).contains("LÉGALITÉ INTERNE");
        assertThat(prompt).contains("PAR CES MOTIFS");
        assertThat(prompt).contains("enjoindre");
    }
}
