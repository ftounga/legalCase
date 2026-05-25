package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MajeursProtegesToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new MajeursProtegesToolBranchRegistry().knownBranches())
                .containsExactly("F-FA-25-majeurs-proteges:default");
    }
}
