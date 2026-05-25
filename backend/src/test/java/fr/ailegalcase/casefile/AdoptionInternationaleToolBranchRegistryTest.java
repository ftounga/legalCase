package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AdoptionInternationaleToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new AdoptionInternationaleToolBranchRegistry().knownBranches())
                .containsExactly("F-FA-ADOPTION-INTERNATIONALE:default");
    }
}
