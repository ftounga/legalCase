package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PartageJudiciaireToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new PartageJudiciaireToolBranchRegistry().knownBranches())
                .containsExactly("F-FA-17-partage-judiciaire:default");
    }
}
