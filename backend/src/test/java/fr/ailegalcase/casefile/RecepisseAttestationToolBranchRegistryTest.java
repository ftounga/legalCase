package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RecepisseAttestationToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        RecepisseAttestationToolBranchRegistry registry = new RecepisseAttestationToolBranchRegistry();

        assertThat(registry.knownBranches())
                .containsExactly("F-IM-32-recepisse-attestation-fr:default");
    }
}
