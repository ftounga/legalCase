package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-JU-03 / SF-219-17 — vérifie que le registre déclare la branche
 * {@code clause-ecolage-be:default} attendue par le mapping
 * jurisprudence.
 */
class ClauseEcolageBeToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new ClauseEcolageBeToolBranchRegistry().knownBranches())
                .containsExactly("clause-ecolage-be:default");
    }
}
