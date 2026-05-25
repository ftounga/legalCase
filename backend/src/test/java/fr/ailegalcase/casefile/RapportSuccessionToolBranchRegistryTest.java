package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RapportSuccessionToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new RapportSuccessionToolBranchRegistry().knownBranches())
                .containsExactly("F-FA-24-rapport-succession:default");
    }
}
