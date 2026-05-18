package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-37 — tests de la cellule CA_FAM / APPEL / INTIME : combinaison
 * déclarée et prompt système (rôle intimé, chambre de la famille de la cour
 * d'appel, dispositif « CONFIRMER » et appel incident).
 */
class CaFamAppelIntimePromptProviderTest {

    private final CaFamAppelIntimePromptProvider provider = new CaFamAppelIntimePromptProvider();

    @Test
    void combination_isFamilyLawFranceFamilyAppealCourtRespondent() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_FAMILLE);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.FRANCE);
        assertThat(key.jurisdiction()).isEqualTo("CA_FAM");
        assertThat(key.stage()).isEqualTo("APPEL");
        assertThat(key.position()).isEqualTo("INTIME");
    }

    @Test
    void systemPrompt_targetsFamilyAppealCourtRespondentRoleAndConfirmationRelief() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("chambre de la famille");
        assertThat(prompt).contains("cour d'appel");
        assertThat(prompt).contains("intimé");
        assertThat(prompt).contains("article 954");
        assertThat(prompt).contains("CONFIRMER");
        assertThat(prompt).contains("APPEL INCIDENT");
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
