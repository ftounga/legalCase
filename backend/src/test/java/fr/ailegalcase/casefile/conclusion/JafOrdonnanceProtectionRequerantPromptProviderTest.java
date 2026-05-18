package fr.ailegalcase.casefile.conclusion;

import fr.ailegalcase.casefile.ProcedureStageCatalog;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-98 / SF-98-35 — tests de la cellule JAF / ORDONNANCE_PROTECTION / REQUERANT :
 * combinaison déclarée et marqueurs du prompt système (JAF, ordonnance de
 * protection, art. 515-9 du code civil, vraisemblance des violences et du danger).
 */
class JafOrdonnanceProtectionRequerantPromptProviderTest {

    private final JafOrdonnanceProtectionRequerantPromptProvider provider =
            new JafOrdonnanceProtectionRequerantPromptProvider();

    @Test
    void combination_isFamilyLawFranceJafProtectionOrderClaimant() {
        CombinationKey key = provider.combination();

        assertThat(key.domain()).isEqualTo(ProcedureStageCatalog.DROIT_FAMILLE);
        assertThat(key.country()).isEqualTo(ProcedureStageCatalog.FRANCE);
        assertThat(key.jurisdiction()).isEqualTo("JAF");
        assertThat(key.stage()).isEqualTo("ORDONNANCE_PROTECTION");
        assertThat(key.position()).isEqualTo("REQUERANT");
    }

    @Test
    void systemPrompt_targetsJafProtectionOrderWithLightenedEvidenceStandard() {
        String prompt = provider.systemPrompt();

        assertThat(prompt).contains("juge aux affaires familiales");
        assertThat(prompt).contains("JAF");
        assertThat(prompt).contains("ordonnance de protection");
        assertThat(prompt).contains("515-9");
        assertThat(prompt).contains("vraisemblance");
        assertThat(prompt).contains("danger");
    }
}
