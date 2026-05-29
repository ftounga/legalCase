package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VictimeTraiteToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        VictimeTraiteToolBranchRegistry registry = new VictimeTraiteToolBranchRegistry();

        assertThat(registry.knownBranches())
                .containsExactly("F-IM-35-victime-traite-l4251-fr:default");
    }
}
