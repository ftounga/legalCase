package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-JU-03 / SF-219-03 — vérifie que le registre déclare la branche
 * {@code rcc-be-entreprise-difficulte:default} attendue par le mapping
 * jurisprudence.
 */
class RccBeEntrepriseDifficulteToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new RccBeEntrepriseDifficulteToolBranchRegistry()
                .knownBranches())
                .containsExactly("rcc-be-entreprise-difficulte:default");
    }
}
