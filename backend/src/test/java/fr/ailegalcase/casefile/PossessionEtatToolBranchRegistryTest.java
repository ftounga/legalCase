package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PossessionEtatToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new PossessionEtatToolBranchRegistry().knownBranches())
                .containsExactly("F-FA-18-possession-etat:default");
    }
}
