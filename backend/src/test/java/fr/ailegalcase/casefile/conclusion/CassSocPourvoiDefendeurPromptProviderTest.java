package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-10 — tests de la cellule CASS_SOC / POURVOI / DEFENDEUR_POURVOI :
 * combinaison déclarée et prompt système (mémoire en défense, réfutation moyen
 * par moyen, irrecevabilité, rejet du pourvoi, pas de demandes chiffrées).
 */
class CassSocPourvoiDefendeurPromptProviderTest {

    private final CassSocPourvoiDefendeurPromptProvider provider =
            new CassSocPourvoiDefendeurPromptProvider();

    @Test
    void combination_isCassationSocialeFrancePourvoiDefendant() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_DU_TRAVAIL);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.FRANCE);
        assertThat(key.jurisdiction()).isEqualTo("CASS_SOC");
        assertThat(key.stage()).isEqualTo("POURVOI");
        assertThat(key.position()).isEqualTo("DEFENDEUR_POURVOI");
    }

    @Test
    void systemPrompt_targetsRespondentDefenceMemoirAndPourvoiRejection() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("avocat du défendeur au pourvoi");
        assertThat(prompt).contains("Cour de cassation");
        assertThat(prompt).contains("MÉMOIRE EN DÉFENSE");
        assertThat(prompt).contains("moyen par moyen");
        assertThat(prompt).contains("IRRECEVABILITÉ");
        assertThat(prompt).contains("rejeter le pourvoi");
        assertThat(prompt).contains("PAR CES MOTIFS");
    }

    @Test
    void systemPrompt_judgesInLawAndForbidsMonetaryClaims() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("juge en droit");
        assertThat(prompt).contains("AUCUNE demande chiffrée");
    }

    @Test
    void systemPrompt_keepsTransverseDraftingInstructions() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("Pièce n° X");
        assertThat(prompt).contains("outils décisionnels");
        assertThat(prompt).contains("N'invente aucun chiffre");
        assertThat(prompt).contains("relu par l'avocat");
    }
}
