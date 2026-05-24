package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RegimeAlgerienToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new RegimeAlgerienToolBranchRegistry().knownBranches())
                .containsExactly("F-IM-17-regime-algerien:default");
    }
}
