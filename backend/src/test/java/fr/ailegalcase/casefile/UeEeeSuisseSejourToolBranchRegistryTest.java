package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UeEeeSuisseSejourToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        UeEeeSuisseSejourToolBranchRegistry registry = new UeEeeSuisseSejourToolBranchRegistry();

        assertThat(registry.knownBranches())
                .containsExactly("F-IM-44-ue-eee-suisse-sejour-fr:default");
    }
}
