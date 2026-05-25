package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AripaRecouvrementFrToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new AripaRecouvrementFrToolBranchRegistry().knownBranches())
                .containsExactly("F-FA-ARIPA-RECOUVREMENT:default");
    }
}
