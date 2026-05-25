package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class OrdonnanceProtectionToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new OrdonnanceProtectionToolBranchRegistry().knownBranches())
                .containsExactly("F-FA-14-ordonnance-protection:default");
    }
}
