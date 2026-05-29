package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegroupementFamilialToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        RegroupementFamilialToolBranchRegistry registry = new RegroupementFamilialToolBranchRegistry();

        assertThat(registry.knownBranches())
                .containsExactly("F-IM-26-regroupement-familial-fr:default");
    }
}
