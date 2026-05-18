package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-39 — tests de la cellule TJ / FILIATION / DEFENDEUR :
 * combinaison déclarée et prompt système (rôle défendeur, conclusions en
 * défense — contestation de recevabilité, réfutation des preuves de filiation).
 */
class TjFiliationDefendeurPromptProviderTest {

    private final TjFiliationDefendeurPromptProvider provider = new TjFiliationDefendeurPromptProvider();

    @Test
    void combination_isFamilyLawFranceJudicialTribunalFiliationDefendant() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_FAMILLE);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.FRANCE);
        assertThat(key.jurisdiction()).isEqualTo("TJ");
        assertThat(key.stage()).isEqualTo("FILIATION");
        assertThat(key.position()).isEqualTo("DEFENDEUR");
    }

    @Test
    void systemPrompt_targetsDefendantRoleAndFiliationMarkers() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("avocat du défendeur");
        assertThat(prompt).contains("tribunal judiciaire");
        assertThat(prompt).contains("filiation");
        assertThat(prompt).contains("titre VII");
        assertThat(prompt).contains("code civil");
        assertThat(prompt).contains("recevabilité");
        assertThat(prompt).contains("expertise");
        assertThat(prompt).contains("PROJET DE CONCLUSIONS EN DÉFENSE");
        assertThat(prompt).contains("PAR CES MOTIFS");
        assertThat(prompt).contains("Pièce n° X");
    }
}
