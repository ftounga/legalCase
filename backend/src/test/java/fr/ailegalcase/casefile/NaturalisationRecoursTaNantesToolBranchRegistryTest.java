package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NaturalisationRecoursTaNantesToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        NaturalisationRecoursTaNantesToolBranchRegistry registry =
                new NaturalisationRecoursTaNantesToolBranchRegistry();

        assertThat(registry.knownBranches())
                .containsExactly("F-IM-40-naturalisation-recours-ta-fr:default");
    }
}
