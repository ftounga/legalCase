package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DonationPartageToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new DonationPartageToolBranchRegistry().knownBranches())
                .containsExactly("F-FA-DONATION-PARTAGE:default");
    }
}
