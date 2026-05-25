package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PrestationCompensatoireSF216ToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new PrestationCompensatoireSF216ToolBranchRegistry().knownBranches())
                .containsExactly("F-FA-01-prestation-compensatoire:default");
    }
}
