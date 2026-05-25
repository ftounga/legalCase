package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DonationToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new DonationToolBranchRegistry().knownBranches())
                .containsExactly("F-FA-24-donation:default");
    }
}
