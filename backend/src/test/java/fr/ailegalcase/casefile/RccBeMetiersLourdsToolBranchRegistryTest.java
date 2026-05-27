package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-JU-03 / SF-219-01 — vérifie que le registre déclare la branche
 * {@code rcc-be-metiers-lourds:default} attendue par le mapping
 * jurisprudence.
 */
class RccBeMetiersLourdsToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new RccBeMetiersLourdsToolBranchRegistry().knownBranches())
                .containsExactly("rcc-be-metiers-lourds:default");
    }
}
