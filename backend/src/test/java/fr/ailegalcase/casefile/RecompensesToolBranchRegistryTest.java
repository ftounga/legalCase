package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RecompensesToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new RecompensesToolBranchRegistry().knownBranches())
                .containsExactly("F-FA-15-recompenses:default");
    }
}
