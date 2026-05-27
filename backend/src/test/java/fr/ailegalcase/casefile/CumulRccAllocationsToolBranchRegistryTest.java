package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-JU-03 / SF-219-04 — vérifie que le registre déclare la branche
 * {@code cumul-rcc-allocations:default} attendue par le mapping
 * jurisprudence.
 */
class CumulRccAllocationsToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new CumulRccAllocationsToolBranchRegistry().knownBranches())
                .containsExactly("cumul-rcc-allocations:default");
    }
}
