package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CrrvRefusVisaToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        CrrvRefusVisaToolBranchRegistry registry = new CrrvRefusVisaToolBranchRegistry();

        assertThat(registry.knownBranches())
                .containsExactly("F-IM-23-crrv-refus-visa-fr:default");
    }
}
