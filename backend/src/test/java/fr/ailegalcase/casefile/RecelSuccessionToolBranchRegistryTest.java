package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RecelSuccessionToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new RecelSuccessionToolBranchRegistry().knownBranches())
                .containsExactly("F-FA-RECEL-SUCCESSION:default");
    }
}
