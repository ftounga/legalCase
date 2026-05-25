package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PacsDissolutionToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new PacsDissolutionToolBranchRegistry().knownBranches())
                .containsExactly("F-FA-20-pacs-dissolution:default");
    }
}
