package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-32 — tests de la cellule JAF / MESURES_PROVISOIRES / DEMANDEUR :
 * combinaison déclarée et prompt système (rôle demandeur, mesures provisoires de
 * l'instance en divorce, ancrage code civil art. 254-255).
 */
class JafMesuresProvisoiresDemandeurPromptProviderTest {

    private final JafMesuresProvisoiresDemandeurPromptProvider provider =
            new JafMesuresProvisoiresDemandeurPromptProvider();

    @Test
    void combination_isFamilyFranceJafProvisionalMeasuresClaimant() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_FAMILLE);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.FRANCE);
        assertThat(key.jurisdiction()).isEqualTo("JAF");
        assertThat(key.stage()).isEqualTo("MESURES_PROVISOIRES");
        assertThat(key.position()).isEqualTo("DEMANDEUR");
    }

    @Test
    void systemPrompt_targetsJafProvisionalMeasuresAndCivilCode() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("avocat du demandeur");
        assertThat(prompt).contains("juge aux affaires familiales");
        assertThat(prompt).contains("mesures provisoires");
        assertThat(prompt).contains("art. 254 et 255");
        assertThat(prompt).contains("PROJET DE CONCLUSIONS");
        assertThat(prompt).contains("PAR CES MOTIFS");
    }
}
