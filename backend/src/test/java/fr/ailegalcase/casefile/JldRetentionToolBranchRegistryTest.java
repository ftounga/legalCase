package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JldRetentionToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        JldRetentionToolBranchRegistry registry = new JldRetentionToolBranchRegistry();

        assertThat(registry.knownBranches())
                .containsExactly("F-IM-21-jld-retention-fr:default");
    }
}
