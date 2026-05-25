package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ReserveHereditaireToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new ReserveHereditaireToolBranchRegistry().knownBranches())
                .containsExactly("F-FA-24-reserve-heriditaire:default");
    }
}
