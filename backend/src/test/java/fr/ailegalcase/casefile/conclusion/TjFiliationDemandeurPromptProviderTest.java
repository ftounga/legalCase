package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-39 — tests de la cellule TJ / FILIATION / DEMANDEUR :
 * combinaison déclarée et prompt système (rôle demandeur, ancrage code civil
 * titre VII, modes de preuve de la filiation, expertise génétique).
 */
class TjFiliationDemandeurPromptProviderTest {

    private final TjFiliationDemandeurPromptProvider provider = new TjFiliationDemandeurPromptProvider();

    @Test
    void combination_isFamilyLawFranceJudicialTribunalFiliationClaimant() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_FAMILLE);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.FRANCE);
        assertThat(key.jurisdiction()).isEqualTo("TJ");
        assertThat(key.stage()).isEqualTo("FILIATION");
        assertThat(key.position()).isEqualTo("DEMANDEUR");
    }

    @Test
    void systemPrompt_targetsClaimantRoleAndFiliationMarkers() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("avocat du demandeur");
        assertThat(prompt).contains("tribunal judiciaire");
        assertThat(prompt).contains("filiation");
        assertThat(prompt).contains("titre VII");
        assertThat(prompt).contains("code civil");
        assertThat(prompt).contains("modes de preuve");
        assertThat(prompt).contains("expertise");
        assertThat(prompt).contains("PROJET DE CONCLUSIONS");
        assertThat(prompt).contains("PAR CES MOTIFS");
        assertThat(prompt).contains("Pièce n° X");
    }
}
