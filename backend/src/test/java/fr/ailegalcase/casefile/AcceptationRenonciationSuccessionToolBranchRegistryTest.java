package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AcceptationRenonciationSuccessionToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new AcceptationRenonciationSuccessionToolBranchRegistry().knownBranches())
                .containsExactly("acceptation-renonciation-succession:default");
    }
}
