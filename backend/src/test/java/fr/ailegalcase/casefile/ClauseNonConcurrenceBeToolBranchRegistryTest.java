package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-JU-03 / SF-213-01 — vérifie que le registre déclare la branche
 * {@code clause-non-concurrence-be:default} attendue par le mapping
 * jurisprudence.
 */
class ClauseNonConcurrenceBeToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new ClauseNonConcurrenceBeToolBranchRegistry().knownBranches())
                .containsExactly("clause-non-concurrence-be:default");
    }
}
