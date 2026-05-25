package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class CommunauteUniverselleToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new CommunauteUniverselleToolBranchRegistry().knownBranches())
                .containsExactly("F-FA-16-communaute-universelle:default");
    }
}
