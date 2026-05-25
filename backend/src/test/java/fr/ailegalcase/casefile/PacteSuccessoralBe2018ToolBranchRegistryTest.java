package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PacteSuccessoralBe2018ToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new PacteSuccessoralBe2018ToolBranchRegistry().knownBranches())
                .containsExactly("pacte-successoral-be-2018:default");
    }
}
