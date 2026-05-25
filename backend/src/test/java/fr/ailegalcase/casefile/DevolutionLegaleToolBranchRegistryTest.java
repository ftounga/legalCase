package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DevolutionLegaleToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new DevolutionLegaleToolBranchRegistry().knownBranches())
                .containsExactly("F-FA-24-devolution-legale:default");
    }
}
