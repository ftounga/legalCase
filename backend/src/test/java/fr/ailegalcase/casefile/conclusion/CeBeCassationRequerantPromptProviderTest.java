package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-28 — tests de la cellule CE_BE / CASSATION / REQUERANT :
 * combinaison déclarée et prompt système (cassation administrative belge,
 * arrêt du CCE, ancrage lois coordonnées du 12 janvier 1973, pas de demandes
 * chiffrées, aucune référence au droit français).
 */
class CeBeCassationRequerantPromptProviderTest {

    private final CeBeCassationRequerantPromptProvider provider =
            new CeBeCassationRequerantPromptProvider();

    @Test
    void combination_isImmigrationBelgiumConseilEtatCassationClaimant() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_IMMIGRATION);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.BELGIQUE);
        assertThat(key.jurisdiction()).isEqualTo("CE_BE");
        assertThat(key.stage()).isEqualTo("CASSATION");
        assertThat(key.position()).isEqualTo("REQUERANT");
    }

    @Test
    void systemPrompt_targetsBelgianAdministrativeCassationAgainstCceJudgment() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("Conseil d'État de Belgique");
        assertThat(prompt).contains("lois coordonnées du 12 janvier 1973");
        assertThat(prompt).contains("MOYENS DE CASSATION");
        assertThat(prompt).contains("Conseil du contentieux des étrangers");
        assertThat(prompt).contains("PAR CES MOTIFS");
        assertThat(prompt).contains("CASSER l'arrêt attaqué");
    }

    @Test
    void systemPrompt_isPureLawWithoutMonetaryClaims() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("AUCUNE demande chiffrée");
        assertThat(prompt).contains("CONTRÔLE LA LÉGALITÉ");
        assertThat(prompt).contains("Pièce n° X");
        assertThat(prompt).contains("N'invente aucun chiffre");
    }

    @Test
    void systemPrompt_hasNoFrenchLawReference() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).doesNotContain("code de justice administrative");
        assertThat(prompt).doesNotContain("cour administrative d'appel");
        assertThat(prompt).doesNotContain("CESEDA");
        assertThat(prompt).doesNotContain("France");
    }
}
