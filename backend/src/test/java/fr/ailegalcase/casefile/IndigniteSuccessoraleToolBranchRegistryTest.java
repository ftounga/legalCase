package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class IndigniteSuccessoraleToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new IndigniteSuccessoraleToolBranchRegistry().knownBranches())
                .containsExactly("F-FA-INDIGNITE-SUCCESSORALE:default");
    }
}
