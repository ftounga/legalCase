package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ItfJudiciaireToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        ItfJudiciaireToolBranchRegistry registry = new ItfJudiciaireToolBranchRegistry();

        assertThat(registry.knownBranches())
                .containsExactly("F-IM-43-itf-judiciaire-fr:default");
    }
}
