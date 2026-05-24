package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EtrangerMaladeToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        EtrangerMaladeToolBranchRegistry registry = new EtrangerMaladeToolBranchRegistry();

        assertThat(registry.knownBranches())
                .containsExactly("F-IM-25-etranger-malade-l4259-fr:default");
    }
}
