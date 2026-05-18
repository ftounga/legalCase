package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-07 — tests de la cellule CA_SOC / APPEL / APPELANT : combinaison
 * déclarée et prompt système (rôle appelant, structure d'appel art. 954 CPC —
 * critique du jugement chef par chef et dispositif récapitulatif « INFIRMER »).
 */
class CaSocAppelAppelantPromptProviderTest {

    private final CaSocAppelAppelantPromptProvider provider = new CaSocAppelAppelantPromptProvider();

    @Test
    void combination_isWorkLawFranceCourtOfAppealSocialAppealAppellant() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_DU_TRAVAIL);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.FRANCE);
        assertThat(key.jurisdiction()).isEqualTo("CA_SOC");
        assertThat(key.stage()).isEqualTo("APPEL");
        assertThat(key.position()).isEqualTo("APPELANT");
    }

    @Test
    void systemPrompt_targetsAppellantRoleAndAppealStructure() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("avocat de l'appelant");
        assertThat(prompt).contains("chambre sociale de la cour d'appel");
        assertThat(prompt).contains("CONCLUSIONS D'APPEL");
        assertThat(prompt).contains("article 954");
        assertThat(prompt).contains("jugement déféré");
        assertThat(prompt).contains("chef par chef");
        assertThat(prompt).contains("INFIRMER");
        assertThat(prompt).contains("DISPOSITIF RÉCAPITULATIF");
    }

    @Test
    void systemPrompt_keepsTransverseDraftingInstructions() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("Pièce n° X");
        assertThat(prompt).contains("outils décisionnels");
        assertThat(prompt).contains("Reprends les montants exacts des calculs");
        assertThat(prompt).contains("relu par l'avocat");
    }
}
