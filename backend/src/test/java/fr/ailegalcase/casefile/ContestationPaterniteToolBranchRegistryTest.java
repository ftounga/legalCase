package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ContestationPaterniteToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new ContestationPaterniteToolBranchRegistry().knownBranches())
                .containsExactly("F-FA-18-contestation-paternite:default");
    }
}
