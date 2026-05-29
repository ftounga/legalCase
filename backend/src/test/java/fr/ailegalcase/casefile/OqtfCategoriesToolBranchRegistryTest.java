package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OqtfCategoriesToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        OqtfCategoriesToolBranchRegistry registry = new OqtfCategoriesToolBranchRegistry();

        assertThat(registry.knownBranches())
                .containsExactly("F-IM-29-oqtf-categories-l6111-fr:default");
    }
}
