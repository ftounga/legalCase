package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-40 — tests de la cellule TJ / SUCCESSION / DEMANDEUR : combinaison
 * déclarée et prompt système (rôle demandeur, partage judiciaire, ancrage livre III
 * du code civil).
 */
class TjSuccessionDemandeurPromptProviderTest {

    private final TjSuccessionDemandeurPromptProvider provider = new TjSuccessionDemandeurPromptProvider();

    @Test
    void combination_isFamilyTribunalFranceSuccessionClaimant() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_FAMILLE);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.FRANCE);
        assertThat(key.jurisdiction()).isEqualTo("TJ");
        assertThat(key.stage()).isEqualTo("SUCCESSION");
        assertThat(key.position()).isEqualTo("DEMANDEUR");
    }

    @Test
    void systemPrompt_targetsClaimantRoleAndJudicialPartition() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("avocat du demandeur");
        assertThat(prompt).contains("tribunal judiciaire");
        assertThat(prompt).contains("partage judiciaire");
        assertThat(prompt).contains("livre III");
        assertThat(prompt).contains("code civil");
        assertThat(prompt).contains("masse partageable");
        assertThat(prompt).contains("PROJET DE CONCLUSIONS");
        assertThat(prompt).contains("PAR CES MOTIFS");
    }

    @Test
    void systemPrompt_keepsTransverseDraftingInstructions() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("Pièce n° X");
        assertThat(prompt).contains("outils décisionnels");
        assertThat(prompt).contains("n'invente aucun chiffre");
        assertThat(prompt).contains("relu par l'avocat");
    }
}
