package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-JU-03 / SF-219-26 — vérifie que le registre déclare la branche
 * {@code travail-noir-be-dimona:default} attendue par le mapping
 * jurisprudence.
 */
class TravailNoirBeDimonaToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(
                new TravailNoirBeDimonaToolBranchRegistry().knownBranches())
                .containsExactly("travail-noir-be-dimona:default");
    }
}
