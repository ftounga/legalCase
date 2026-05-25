package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RegimeCommunauteLegaleBeToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new RegimeCommunauteLegaleBeToolBranchRegistry().knownBranches())
                .containsExactly("regime-mat-be-communaute-legale:default");
    }
}
