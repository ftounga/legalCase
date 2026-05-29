package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CarteResidentToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        CarteResidentToolBranchRegistry registry = new CarteResidentToolBranchRegistry();

        assertThat(registry.knownBranches())
                .containsExactly("F-IM-36-carte-resident-l4261-fr:default");
    }
}
