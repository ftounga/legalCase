package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class IndivisionToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new IndivisionToolBranchRegistry().knownBranches())
                .containsExactly("F-FA-22-indivision:default");
    }
}
