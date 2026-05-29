package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VpfLiensPersonnelsToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        VpfLiensPersonnelsToolBranchRegistry registry = new VpfLiensPersonnelsToolBranchRegistry();

        assertThat(registry.knownBranches())
                .containsExactly("F-IM-27-vpf-liens-personnels-l42323-fr:default");
    }
}
