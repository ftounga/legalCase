package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AutoriteParentaleBeToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new AutoriteParentaleBeToolBranchRegistry().knownBranches())
                .containsExactly("autorite-parentale-be:default");
    }
}
