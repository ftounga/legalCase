package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-JU-03 / SF-219-02 — vérifie que le registre déclare la branche
 * {@code rcc-be-longue-carriere:default} attendue par le mapping
 * jurisprudence.
 */
class RccBeLongueCarriereToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new RccBeLongueCarriereToolBranchRegistry().knownBranches())
                .containsExactly("rcc-be-longue-carriere:default");
    }
}
