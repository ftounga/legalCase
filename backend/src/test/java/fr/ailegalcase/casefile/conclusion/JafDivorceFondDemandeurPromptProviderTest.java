package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-30 — tests de la cellule JAF / DIVORCE_FOND / DEMANDEUR :
 * combinaison déclarée et prompt système (rôle demandeur, divorce contentieux,
 * ancrage code civil, demandes accessoires).
 */
class JafDivorceFondDemandeurPromptProviderTest {

    private final JafDivorceFondDemandeurPromptProvider provider =
            new JafDivorceFondDemandeurPromptProvider();

    @Test
    void combination_isFamilyLawFranceJafDivorceFondClaimant() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_FAMILLE);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.FRANCE);
        assertThat(key.jurisdiction()).isEqualTo("JAF");
        assertThat(key.stage()).isEqualTo("DIVORCE_FOND");
        assertThat(key.position()).isEqualTo("DEMANDEUR");
    }

    @Test
    void systemPrompt_targetsClaimantRoleAndDivorceConclusionStructure() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("avocat du demandeur");
        assertThat(prompt).contains("juge aux affaires familiales");
        assertThat(prompt).contains("divorce");
        assertThat(prompt).contains("PROJET DE CONCLUSIONS");
        assertThat(prompt).contains("PAR CES MOTIFS");
    }

    @Test
    void systemPrompt_anchorsCivilCodeAndAccessoryClaims() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("code civil");
        assertThat(prompt).contains("art. 233");
        assertThat(prompt).contains("prestation compensatoire");
        assertThat(prompt).contains("autorité parentale");
        assertThat(prompt).contains("Pièce n° X");
    }
}
