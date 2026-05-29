package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OfpraIntroductionToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        OfpraIntroductionToolBranchRegistry registry = new OfpraIntroductionToolBranchRegistry();

        assertThat(registry.knownBranches())
                .containsExactly("F-IM-33-ofpra-introduction-fr:default");
    }
}
