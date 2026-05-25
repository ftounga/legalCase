package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RetraitApToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new RetraitApToolBranchRegistry().knownBranches())
                .containsExactly("F-FA-RETRAIT-AP:default");
    }
}
