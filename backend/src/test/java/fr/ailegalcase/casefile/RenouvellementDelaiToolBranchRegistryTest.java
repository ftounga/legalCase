package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RenouvellementDelaiToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        RenouvellementDelaiToolBranchRegistry registry = new RenouvellementDelaiToolBranchRegistry();

        assertThat(registry.knownBranches())
                .containsExactly("F-IM-31-renouvellement-delai-depot-fr:default");
    }
}
