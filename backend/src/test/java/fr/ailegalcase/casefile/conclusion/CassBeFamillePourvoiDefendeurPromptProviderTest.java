package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-45 — tests de la cellule Cour de cassation de Belgique / POURVOI /
 * DEFENDEUR_POURVOI en droit de la famille : combinaison déclarée et prompt système
 * (mémoire en réponse, réfutation moyen par moyen, irrecevabilité, rejet du pourvoi,
 * pas de demande chiffrée).
 */
class CassBeFamillePourvoiDefendeurPromptProviderTest {

    private final CassBeFamillePourvoiDefendeurPromptProvider provider =
            new CassBeFamillePourvoiDefendeurPromptProvider();

    @Test
    void combination_isFamilyLawBelgiumCassationPourvoiDefendant() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_FAMILLE);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.BELGIQUE);
        assertThat(key.jurisdiction()).isEqualTo("CASS_BE");
        assertThat(key.stage()).isEqualTo("POURVOI");
        assertThat(key.position()).isEqualTo("DEFENDEUR_POURVOI");
    }

    @Test
    void systemPrompt_targetsBelgianResponseMemoirAndDismissalOfPourvoi() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("Cour de cassation de Belgique");
        assertThat(prompt).contains("avocat du défendeur en cassation");
        assertThat(prompt).contains("MÉMOIRE EN RÉPONSE");
        assertThat(prompt).contains("Code judiciaire");
        assertThat(prompt).contains("art. 1073");
        assertThat(prompt).contains("MOYEN PAR MOYEN");
        assertThat(prompt).contains("IRRECEVABILITÉ");
        assertThat(prompt).contains("REJET DU POURVOI");
        assertThat(prompt).contains("Code civil belge");
        assertThat(prompt).contains("chambre de la famille");
    }

    @Test
    void systemPrompt_isPureLawNoFiguredClaim() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("JUGE EN DROIT");
        assertThat(prompt).contains("AUCUNE demande chiffrée");
        assertThat(prompt).contains("N'invente aucun chiffre");
    }

    @Test
    void systemPrompt_hasNoFrenchLawReference() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).doesNotContain("mémoire ampliatif");
        assertThat(prompt).doesNotContain("chambre civile");
    }

    @Test
    void systemPrompt_keepsTransverseDraftingInstructions() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("Pièce n° X");
        assertThat(prompt).contains("outils décisionnels");
        assertThat(prompt).contains("relu par l'avocat");
    }
}
