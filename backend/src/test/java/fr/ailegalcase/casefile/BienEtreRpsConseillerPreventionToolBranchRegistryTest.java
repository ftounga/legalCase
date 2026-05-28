package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-JU-03 / SF-219-30 — vérifie que le registre déclare la branche
 * {@code bien-etre-rps-conseiller-prevention:default} attendue par le
 * mapping jurisprudence.
 */
class BienEtreRpsConseillerPreventionToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(
                new BienEtreRpsConseillerPreventionToolBranchRegistry()
                        .knownBranches())
                .containsExactly(
                        "bien-etre-rps-conseiller-prevention:default");
    }
}
