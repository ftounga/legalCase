package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ChangementResidenceToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new ChangementResidenceToolBranchRegistry().knownBranches())
                .containsExactly("F-FA-19-changement-residence:default");
    }
}
