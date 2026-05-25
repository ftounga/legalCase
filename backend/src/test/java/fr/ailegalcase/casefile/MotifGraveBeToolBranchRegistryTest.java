package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MotifGraveBeToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new MotifGraveBeToolBranchRegistry().knownBranches())
                .containsExactly("F-DT-27-motif-grave-be:default");
    }
}
