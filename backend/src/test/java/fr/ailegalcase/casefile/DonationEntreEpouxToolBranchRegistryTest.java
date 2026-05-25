package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class DonationEntreEpouxToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new DonationEntreEpouxToolBranchRegistry().knownBranches())
                .containsExactly("F-FA-DONATION-ENTRE-EPOUX:default");
    }
}
