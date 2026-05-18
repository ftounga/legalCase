package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-38 — tests de la cellule CASS_CIV1 / POURVOI / DEMANDEUR_POURVOI
 * (famille FR) : combinaison déclarée et prompt système (mémoire ampliatif, moyens
 * de cassation, cas d'ouverture, dispositif « casser et annuler », document de pur
 * droit sans demande chiffrée).
 */
class CassCiv1PourvoiDemandeurPromptProviderTest {

    private final CassCiv1PourvoiDemandeurPromptProvider provider =
            new CassCiv1PourvoiDemandeurPromptProvider();

    @Test
    void combination_isFamilyCassationCiv1PourvoiClaimant() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_FAMILLE);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.FRANCE);
        assertThat(key.jurisdiction()).isEqualTo("CASS_CIV1");
        assertThat(key.stage()).isEqualTo("POURVOI");
        assertThat(key.position()).isEqualTo("DEMANDEUR_POURVOI");
    }

    @Test
    void systemPrompt_targetsAmpliatoryBriefAndCassationGrounds() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("avocat du demandeur au pourvoi");
        assertThat(prompt).contains("1ʳᵉ chambre civile de la Cour de cassation");
        assertThat(prompt).contains("MÉMOIRE AMPLIATIF");
        assertThat(prompt).contains("MOYEN(S) DE CASSATION");
        assertThat(prompt).contains("BRANCHES");
        assertThat(prompt).contains("CAS D'OUVERTURE À CASSATION");
        assertThat(prompt).contains("ARRÊT ATTAQUÉ");
        assertThat(prompt).contains("CASSER ET ANNULER");
    }

    @Test
    void systemPrompt_neutralizesNumericClaimsAsCourtRulesOnLaw() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("JUGE EN DROIT");
        assertThat(prompt).contains("AUCUNE demande chiffrée");
        assertThat(prompt).contains("N'invente aucun chiffre");
    }

    @Test
    void systemPrompt_keepsTransverseDraftingInstructions() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("Pièce n° X");
        assertThat(prompt).contains("outils décisionnels");
        assertThat(prompt).contains("relu par l'avocat");
    }
}
