package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class OrdonnanceRequeteToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new OrdonnanceRequeteToolBranchRegistry().knownBranches())
                .containsExactly("F-FA-23-ordonnance-requete:default");
    }
}
