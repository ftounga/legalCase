package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MineursImmigrationToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new MineursImmigrationToolBranchRegistry().knownBranches())
                .containsExactly("F-IM-19-mineurs:default");
    }
}
