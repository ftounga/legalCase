package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-JU-03 / SF-219-32 — vérifie que le registre déclare la branche
 * {@code interruption-carriere-soins-parental:default} attendue par le
 * mapping jurisprudence.
 */
class InterruptionCarriereSoinsParentalToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(
                new InterruptionCarriereSoinsParentalToolBranchRegistry()
                        .knownBranches())
                .containsExactly(
                        "interruption-carriere-soins-parental:default");
    }
}
