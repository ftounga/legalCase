package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-01 + SF-98-02 — tests de la cellule CPH / FOND / DEMANDEUR :
 * combinaison déclarée et prompt système (rôle demandeur, structure de conclusions).
 */
class CphFondDemandeurPromptProviderTest {

    private final CphFondDemandeurPromptProvider provider = new CphFondDemandeurPromptProvider();

    @Test
    void combination_isWorkTribunalFranceFondClaimant() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_DU_TRAVAIL);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.FRANCE);
        assertThat(key.jurisdiction()).isEqualTo("CPH");
        assertThat(key.stage()).isEqualTo("FOND");
        assertThat(key.position()).isEqualTo("DEMANDEUR");
    }

    @Test
    void systemPrompt_targetsClaimantRoleAndConclusionStructure() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("avocat du demandeur");
        assertThat(prompt).contains("Conseil de prud'hommes");
        assertThat(prompt).contains("PROJET DE CONCLUSIONS");
        assertThat(prompt).contains("PAR CES MOTIFS");
    }
}
