package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AncienneteToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranchForAnciennete() {
        AncienneteToolBranchRegistry registry = new AncienneteToolBranchRegistry();

        assertThat(registry.knownBranches())
                .containsExactly("F-DT-07-anciennete-conges-prime:default");
    }
}
