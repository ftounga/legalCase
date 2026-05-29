package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AjCndaToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        AjCndaToolBranchRegistry registry = new AjCndaToolBranchRegistry();

        assertThat(registry.knownBranches())
                .containsExactly("F-IM-34-aj-cnda-fr:default");
    }
}
