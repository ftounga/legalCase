package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class LiquidationPartageBeToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new LiquidationPartageBeToolBranchRegistry().knownBranches())
                .containsExactly("liquidation-partage-be:default");
    }
}
