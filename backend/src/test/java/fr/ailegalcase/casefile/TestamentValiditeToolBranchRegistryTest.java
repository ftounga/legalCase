package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TestamentValiditeToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new TestamentValiditeToolBranchRegistry().knownBranches())
                .containsExactly("F-FA-24-testament-validite:default");
    }
}
