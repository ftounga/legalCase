package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-22 — tests de la cellule CE / CASSATION / REQUERANT :
 * combinaison déclarée et prompt système (pourvoi en cassation devant le
 * Conseil d'État, cas d'ouverture, contrôle du droit, pas de demandes chiffrées).
 */
class CeCassationRequerantPromptProviderTest {

    private final CeCassationRequerantPromptProvider provider = new CeCassationRequerantPromptProvider();

    @Test
    void combination_isImmigrationFranceConseilEtatCassationRequerant() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_IMMIGRATION);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.FRANCE);
        assertThat(key.jurisdiction()).isEqualTo("CE");
        assertThat(key.stage()).isEqualTo("CASSATION");
        assertThat(key.position()).isEqualTo("REQUERANT");
    }

    @Test
    void systemPrompt_targetsConseilEtatCassationGrounds() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("Conseil d'État");
        assertThat(prompt).contains("REQUÊTE EN CASSATION");
        assertThat(prompt).contains("MOYENS DE CASSATION");
        assertThat(prompt).contains("cas d'ouverture");
        assertThat(prompt).contains("erreur de droit");
        assertThat(prompt).contains("dénaturation");
        assertThat(prompt).contains("PAR CES MOTIFS");
    }

    @Test
    void systemPrompt_controlsLawWithoutMonetaryClaims() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("contrôle le droit");
        assertThat(prompt).contains("AUCUNE demande chiffrée");
        assertThat(prompt).contains("Pièce n° X");
        assertThat(prompt).contains("relu par l'avocat");
    }
}
