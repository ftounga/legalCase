package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AtFedrisDeclarationToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new AtFedrisDeclarationToolBranchRegistry().knownBranches())
                .containsExactly("at-fedris-declaration:default");
    }
}
