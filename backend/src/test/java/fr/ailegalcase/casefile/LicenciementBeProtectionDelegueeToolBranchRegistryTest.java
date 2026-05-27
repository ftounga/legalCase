package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-JU-03 / SF-213-08 — vérifie que le registre déclare la branche
 * {@code licenciement-be-protection-deleguee:default} attendue par le
 * mapping jurisprudence.
 */
class LicenciementBeProtectionDelegueeToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new LicenciementBeProtectionDelegueeToolBranchRegistry().knownBranches())
                .containsExactly("licenciement-be-protection-deleguee:default");
    }
}
