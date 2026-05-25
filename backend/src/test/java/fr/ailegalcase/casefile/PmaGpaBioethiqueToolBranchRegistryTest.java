package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PmaGpaBioethiqueToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new PmaGpaBioethiqueToolBranchRegistry().knownBranches())
                .containsExactly("F-FA-27-pma-gpa:default");
    }
}
