package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DublinRecoursToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        DublinRecoursToolBranchRegistry registry = new DublinRecoursToolBranchRegistry();

        assertThat(registry.knownBranches())
                .containsExactly("F-IM-22-dublin-recours-fr:default");
    }
}
