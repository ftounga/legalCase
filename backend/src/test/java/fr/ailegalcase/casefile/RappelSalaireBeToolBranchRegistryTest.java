package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-JU-03 / SF-213-02 — vérifie que le registre déclare la branche
 * {@code rappel-salaire-be:default} attendue par le mapping jurisprudence.
 */
class RappelSalaireBeToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new RappelSalaireBeToolBranchRegistry().knownBranches())
                .containsExactly("rappel-salaire-be:default");
    }
}
