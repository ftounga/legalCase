package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class VlsTsValidationToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        VlsTsValidationToolBranchRegistry registry = new VlsTsValidationToolBranchRegistry();

        assertThat(registry.knownBranches())
                .containsExactly("F-IM-28-vls-ts-validation-ofii-fr:default");
    }
}
