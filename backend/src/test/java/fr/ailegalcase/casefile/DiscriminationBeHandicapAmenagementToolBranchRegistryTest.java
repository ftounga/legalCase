package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-JU-03 / SF-219-23 — vérifie que le registre déclare la branche
 * {@code discrimination-be-handicap-amenagement:default} attendue par
 * le mapping jurisprudence.
 */
class DiscriminationBeHandicapAmenagementToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(
                new DiscriminationBeHandicapAmenagementToolBranchRegistry().knownBranches())
                .containsExactly("discrimination-be-handicap-amenagement:default");
    }
}
