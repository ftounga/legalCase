package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class IndivisionSuccessoraleToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new IndivisionSuccessoraleToolBranchRegistry().knownBranches())
                .containsExactly("F-FA-24-indivision-successorale:default");
    }
}
