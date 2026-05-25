package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PartageSuccessoralToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new PartageSuccessoralToolBranchRegistry().knownBranches())
                .containsExactly("F-FA-24-partage-successoral:default");
    }
}
