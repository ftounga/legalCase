package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RccBeConditionsToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new RccBeConditionsToolBranchRegistry().knownBranches())
                .containsExactly("rcc-be-conditions:default");
    }
}
