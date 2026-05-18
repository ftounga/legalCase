package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-08 — tests de la cellule CA_SOC / APPEL / INTIME : combinaison
 * déclarée et prompt système (rôle intimé, dispositif « CONFIRMER », appel incident).
 */
class CaSocAppelIntimePromptProviderTest {

    private final CaSocAppelIntimePromptProvider provider = new CaSocAppelIntimePromptProvider();

    @Test
    void combination_isWorkLawFranceCourtOfAppealSocialAppealRespondent() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_DU_TRAVAIL);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.FRANCE);
        assertThat(key.jurisdiction()).isEqualTo("CA_SOC");
        assertThat(key.stage()).isEqualTo("APPEL");
        assertThat(key.position()).isEqualTo("INTIME");
    }

    @Test
    void systemPrompt_targetsRespondentRoleConfirmationAndCrossAppeal() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("avocat de l'intimé");
        assertThat(prompt).contains("chambre sociale de la cour d'appel");
        assertThat(prompt).contains("CONCLUSIONS D'INTIMÉ");
        assertThat(prompt).contains("article 954");
        assertThat(prompt).contains("jugement déféré");
        assertThat(prompt).contains("CONFIRMER");
        assertThat(prompt).contains("APPEL INCIDENT");
        assertThat(prompt).contains("DISPOSITIF RÉCAPITULATIF");
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
