package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AdoptionToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new AdoptionToolBranchRegistry().knownBranches())
                .containsExactly("F-FA-18-adoption:default");
    }
}
