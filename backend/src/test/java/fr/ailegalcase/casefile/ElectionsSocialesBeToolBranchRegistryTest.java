package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-JU-03 / SF-219-09 — vérifie que le registre déclare la branche
 * {@code elections-sociales-be:default} attendue par le mapping
 * jurisprudence.
 */
class ElectionsSocialesBeToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new ElectionsSocialesBeToolBranchRegistry()
                .knownBranches())
                .containsExactly("elections-sociales-be:default");
    }
}
