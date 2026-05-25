package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class SuccessionBeAcceptationRenonciationToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new SuccessionBeAcceptationRenonciationToolBranchRegistry().knownBranches())
                .containsExactly("succession-be-acceptation-renonciation:default");
    }
}
