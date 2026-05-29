package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AppelCaaCassationToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        AppelCaaCassationToolBranchRegistry registry = new AppelCaaCassationToolBranchRegistry();

        assertThat(registry.knownBranches())
                .containsExactly("F-IM-41-appel-caa-cassation-ce-fr:default");
    }
}
