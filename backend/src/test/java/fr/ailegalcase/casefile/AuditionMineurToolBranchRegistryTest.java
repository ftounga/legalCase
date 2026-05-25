package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class AuditionMineurToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new AuditionMineurToolBranchRegistry().knownBranches())
                .containsExactly("F-FA-AUDITION-MINEUR:default");
    }
}
