package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class C4OnemChecklistToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new C4OnemChecklistToolBranchRegistry().knownBranches())
                .containsExactly("c4-onem-checklist:default");
    }
}
