package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PartageNotarialToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new PartageNotarialToolBranchRegistry().knownBranches())
                .containsExactly("F-FA-PARTAGE-NOTARIAL:default");
    }
}
