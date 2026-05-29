package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NaturalisationRecoursTjToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        NaturalisationRecoursTjToolBranchRegistry registry = new NaturalisationRecoursTjToolBranchRegistry();

        assertThat(registry.knownBranches())
                .containsExactly("F-IM-39-naturalisation-recours-tj-fr:default");
    }
}
