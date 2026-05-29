package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AesPresenceProuveeToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        AesPresenceProuveeToolBranchRegistry registry = new AesPresenceProuveeToolBranchRegistry();

        assertThat(registry.knownBranches())
                .containsExactly("F-IM-30-aes-presence-prouvee-fr:default");
    }
}
