package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-JU-03 / SF-219-15 — vérifie que le registre déclare la branche
 * {@code interim-be-indemnite-fin-mission:default} attendue par le
 * mapping jurisprudence.
 */
class InterimBeIndemniteFinMissionToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new InterimBeIndemniteFinMissionToolBranchRegistry()
                .knownBranches())
                .containsExactly("interim-be-indemnite-fin-mission:default");
    }
}
