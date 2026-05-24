package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VictimeViolencesL4256ToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        VictimeViolencesL4256ToolBranchRegistry registry = new VictimeViolencesL4256ToolBranchRegistry();

        assertThat(registry.knownBranches())
                .containsExactly("F-IM-24-victime-violences-l4256-fr:default");
    }
}
