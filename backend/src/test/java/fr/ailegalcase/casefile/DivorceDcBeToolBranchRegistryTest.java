package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DivorceDcBeToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new DivorceDcBeToolBranchRegistry().knownBranches())
                .containsExactly("divorce-dc-be:default");
    }
}
