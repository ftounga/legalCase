package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AsileAvanceToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new AsileAvanceToolBranchRegistry().knownBranches())
                .containsExactly("F-IM-12-asile-avance:default");
    }
}
