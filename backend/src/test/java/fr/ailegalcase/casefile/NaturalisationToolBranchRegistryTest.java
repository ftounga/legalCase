package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class NaturalisationToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new NaturalisationToolBranchRegistry().knownBranches())
                .containsExactly("F-IM-13-naturalisation:default");
    }
}
