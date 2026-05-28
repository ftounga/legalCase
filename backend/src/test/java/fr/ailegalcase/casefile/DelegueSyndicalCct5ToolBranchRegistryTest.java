package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-JU-03 / SF-219-10 — vérifie que le registre déclare la branche
 * {@code delegue-syndical-cct-5:default} attendue par le mapping
 * jurisprudence.
 */
class DelegueSyndicalCct5ToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new DelegueSyndicalCct5ToolBranchRegistry()
                .knownBranches())
                .containsExactly("delegue-syndical-cct-5:default");
    }
}
