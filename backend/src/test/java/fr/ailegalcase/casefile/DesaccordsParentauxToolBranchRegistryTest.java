package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DesaccordsParentauxToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new DesaccordsParentauxToolBranchRegistry().knownBranches())
                .containsExactly("F-FA-19-desaccords-parentaux:default");
    }
}
