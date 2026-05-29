package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RetraitTitreFraudeToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        RetraitTitreFraudeToolBranchRegistry registry = new RetraitTitreFraudeToolBranchRegistry();

        assertThat(registry.knownBranches())
                .containsExactly("F-IM-45-retrait-titre-fraude-fr:default");
    }
}
