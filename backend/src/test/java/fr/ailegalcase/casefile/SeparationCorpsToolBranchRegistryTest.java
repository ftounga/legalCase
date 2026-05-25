package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SeparationCorpsToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new SeparationCorpsToolBranchRegistry().knownBranches())
                .containsExactly("F-FA-21-separation-corps:default");
    }
}
