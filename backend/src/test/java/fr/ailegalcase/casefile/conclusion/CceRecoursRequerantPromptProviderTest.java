package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-26 — tests de la cellule CCE / RECOURS_PLEIN_CONTENTIEUX /
 * REQUERANT : combinaison déclarée et prompt système (recours belge de plein
 * contentieux devant le Conseil du contentieux des étrangers, loi du 15 décembre
 * 1980, protection internationale). Vérifie l'absence de toute référence au
 * droit français des étrangers.
 */
class CceRecoursRequerantPromptProviderTest {

    private final CceRecoursRequerantPromptProvider provider = new CceRecoursRequerantPromptProvider();

    @Test
    void combination_isImmigrationBelgiumCcePleinContentieuxClaimant() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_IMMIGRATION);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.BELGIQUE);
        assertThat(key.jurisdiction()).isEqualTo("CCE");
        assertThat(key.stage()).isEqualTo("RECOURS_PLEIN_CONTENTIEUX");
        assertThat(key.position()).isEqualTo("REQUERANT");
    }

    @Test
    void systemPrompt_targetsBelgianPleinContentieuxMarkers() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("Conseil du contentieux des étrangers");
        assertThat(prompt).contains("loi du 15 décembre 1980");
        assertThat(prompt).contains("PLEIN CONTENTIEUX");
        assertThat(prompt).contains("PROTECTION INTERNATIONALE");
        assertThat(prompt).contains("Commissariat général aux réfugiés et aux apatrides");
        assertThat(prompt).contains("convention de Genève du 28 juillet 1951");
    }

    @Test
    void systemPrompt_hasNoFrenchImmigrationLawReference() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).doesNotContain("CNDA");
        assertThat(prompt).doesNotContain("CESEDA");
        assertThat(prompt).doesNotContain("OFPRA");
        assertThat(prompt).doesNotContain("tribunal administratif");
        assertThat(prompt).doesNotContain("Conseil d'État");
    }
}
