package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-27 — tests de la cellule CCE / REFERE_EXTREME_URGENCE / REQUERANT :
 * combinaison déclarée et prompt système (recours en suspension d'extrême urgence
 * belge, ancrage loi du 15 décembre 1980, absence de référence au droit français).
 */
class CceRefereExtremeUrgenceRequerantPromptProviderTest {

    private final CceRefereExtremeUrgenceRequerantPromptProvider provider =
            new CceRefereExtremeUrgenceRequerantPromptProvider();

    @Test
    void combination_isImmigrationBelgiumCceExtremeUrgencyClaimant() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_IMMIGRATION);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.BELGIQUE);
        assertThat(key.jurisdiction()).isEqualTo("CCE");
        assertThat(key.stage()).isEqualTo("REFERE_EXTREME_URGENCE");
        assertThat(key.position()).isEqualTo("REQUERANT");
    }

    @Test
    void systemPrompt_targetsBelgianExtremeUrgencySuspensionBeforeCce() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("Conseil du contentieux des étrangers");
        assertThat(prompt).contains("CCE");
        assertThat(prompt).contains("extrême urgence");
        assertThat(prompt).contains("loi du 15 décembre 1980");
        assertThat(prompt).contains("39/82");
        assertThat(prompt).contains("MOYEN SÉRIEUX");
        assertThat(prompt).contains("PRÉJUDICE GRAVE DIFFICILEMENT RÉPARABLE");
        assertThat(prompt).contains("PAR CES MOTIFS");
        assertThat(prompt).contains("Pièce n° X");
    }

    @Test
    void systemPrompt_anchorsBelgianLawAndDoesNotInvokeFrenchInstitutions() {
        String prompt = provider.systemPrompt().toLowerCase();

        // Le prompt doit interdire explicitement le droit français...
        assertThat(prompt).contains("n'invoque jamais le droit français");
        // ...sans jamais ancrer le recours sur une institution ou un code français.
        assertThat(prompt).doesNotContain("tribunal administratif");
        assertThat(prompt).doesNotContain("ceseda");
        assertThat(prompt).doesNotContain("préfet");
        assertThat(prompt).doesNotContain("obligation de quitter le territoire français");
        assertThat(prompt).doesNotContain("code de l'entrée et du séjour");
    }
}
