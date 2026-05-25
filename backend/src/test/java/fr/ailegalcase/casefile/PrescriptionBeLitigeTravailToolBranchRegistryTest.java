package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class PrescriptionBeLitigeTravailToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new PrescriptionBeLitigeTravailToolBranchRegistry().knownBranches())
                .containsExactly("prescription-be-litige-travail:default");
    }
}
