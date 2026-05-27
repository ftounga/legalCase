package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-JU-03 / SF-213-05 — vérifie que le registre déclare la branche
 * {@code licenciement-be-protection-grossesse:default} attendue par le
 * mapping jurisprudence.
 */
class LicenciementBeProtectionGrossesseToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new LicenciementBeProtectionGrossesseToolBranchRegistry().knownBranches())
                .containsExactly("licenciement-be-protection-grossesse:default");
    }
}
