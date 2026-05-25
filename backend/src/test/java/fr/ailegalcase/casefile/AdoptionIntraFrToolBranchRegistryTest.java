package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AdoptionIntraFrToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new AdoptionIntraFrToolBranchRegistry().knownBranches())
                .containsExactly("F-FA-ADOPTION-INTRA:default");
    }
}
