package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-17 — tests de la cellule Cour de cassation de Belgique / POURVOI /
 * DEMANDEUR_POURVOI : combinaison déclarée et prompt système (mémoire à l'appui du
 * pourvoi, moyens de cassation, dispositions violées, cassation, pas de demande
 * chiffrée).
 */
class CassBePourvoiDemandeurPromptProviderTest {

    private final CassBePourvoiDemandeurPromptProvider provider =
            new CassBePourvoiDemandeurPromptProvider();

    @Test
    void combination_isWorkLawBelgiumCassationPourvoiClaimant() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_DU_TRAVAIL);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.BELGIQUE);
        assertThat(key.jurisdiction()).isEqualTo("CASS_BE");
        assertThat(key.stage()).isEqualTo("POURVOI");
        assertThat(key.position()).isEqualTo("DEMANDEUR_POURVOI");
    }

    @Test
    void systemPrompt_targetsBelgianCassationMemoirAndGroundsOfCassation() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("Cour de cassation de Belgique");
        assertThat(prompt).contains("avocat du demandeur en cassation");
        assertThat(prompt).contains("MÉMOIRE À L'APPUI DU POURVOI");
        assertThat(prompt).contains("Code judiciaire");
        assertThat(prompt).contains("art. 1073");
        assertThat(prompt).contains("MOYEN(S) DE CASSATION");
        assertThat(prompt).contains("DISPOSITIONS LÉGALES VIOLÉES");
        assertThat(prompt).contains("CASSER la décision attaquée");
        assertThat(prompt).contains("cour du travail");
    }

    @Test
    void systemPrompt_isPureLawNoFiguredClaim() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("JUGE EN DROIT");
        assertThat(prompt).contains("AUCUNE demande chiffrée");
        assertThat(prompt).contains("N'invente aucun chiffre");
    }

    @Test
    void systemPrompt_isAnchoredInBelgianLawWithoutFrenchReferences() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).doesNotContain("ampliatif");
        assertThat(prompt).doesNotContain("chambre sociale");
        assertThat(prompt).doesNotContain("cour d'appel");
    }

    @Test
    void systemPrompt_keepsTransverseDraftingInstructions() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("Pièce n° X");
        assertThat(prompt).contains("outils décisionnels");
        assertThat(prompt).contains("relu par l'avocat");
    }
}
