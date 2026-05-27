package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-JU-03 / SF-219-05 — vérifie que le registre déclare la branche
 * {@code outplacement-be-general-30sem:default} attendue par le mapping
 * jurisprudence.
 */
class OutplacementBeGeneral30semToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new OutplacementBeGeneral30semToolBranchRegistry()
                .knownBranches())
                .containsExactly("outplacement-be-general-30sem:default");
    }
}
