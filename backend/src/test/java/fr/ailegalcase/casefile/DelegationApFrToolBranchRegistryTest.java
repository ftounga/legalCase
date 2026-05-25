package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DelegationApFrToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new DelegationApFrToolBranchRegistry().knownBranches())
                .containsExactly("F-FA-XX-delegation-ap:default");
    }
}
