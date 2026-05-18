package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-21 — tests de la cellule CAA / APPEL / REQUERANT (droit de
 * l'immigration FR) : combinaison déclarée et marqueurs du prompt système
 * (requête d'appel administratif, jugement du TA attaqué, effet dévolutif).
 */
class CaaAppelRequerantPromptProviderTest {

    private final CaaAppelRequerantPromptProvider provider = new CaaAppelRequerantPromptProvider();

    @Test
    void combination_isImmigrationFranceCaaAppelClaimant() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_IMMIGRATION);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.FRANCE);
        assertThat(key.jurisdiction()).isEqualTo("CAA");
        assertThat(key.stage()).isEqualTo("APPEL");
        assertThat(key.position()).isEqualTo("REQUERANT");
    }

    @Test
    void systemPrompt_targetsAdministrativeAppealAgainstTaJudgment() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("cour administrative d'appel");
        assertThat(prompt).contains("REQUÊTE D'APPEL");
        assertThat(prompt).contains("jugement du tribunal administratif");
        assertThat(prompt).contains("effet dévolutif");
        assertThat(prompt).contains("réformation");
        assertThat(prompt).contains("PAR CES MOTIFS");
        assertThat(prompt).contains("Pièce n° X");
    }
}
