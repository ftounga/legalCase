package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ContestationC4OnemToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new ContestationC4OnemToolBranchRegistry().knownBranches())
                .containsExactly("contestation-c4-onem:default");
    }
}
