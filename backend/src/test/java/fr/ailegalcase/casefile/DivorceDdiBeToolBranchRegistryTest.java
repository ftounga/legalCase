package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DivorceDdiBeToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new DivorceDdiBeToolBranchRegistry().knownBranches())
                .containsExactly("divorce-ddi-3voies-be:default");
    }
}
