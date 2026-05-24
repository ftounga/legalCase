package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AesMetiersTensionToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        AesMetiersTensionToolBranchRegistry registry = new AesMetiersTensionToolBranchRegistry();

        assertThat(registry.knownBranches())
                .containsExactly("F-IM-09-aes-metiers-tension:default");
    }
}
