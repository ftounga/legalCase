package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-JU-03 / SF-219-28 — vérifie que le registre déclare la branche
 * {@code mp-fedris-reconnaissance:default} attendue par le mapping
 * jurisprudence.
 */
class MpFedrisReconnaissanceToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(
                new MpFedrisReconnaissanceToolBranchRegistry().knownBranches())
                .containsExactly("mp-fedris-reconnaissance:default");
    }
}
