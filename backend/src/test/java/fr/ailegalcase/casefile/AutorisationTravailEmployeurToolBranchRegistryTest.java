package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AutorisationTravailEmployeurToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        AutorisationTravailEmployeurToolBranchRegistry registry =
                new AutorisationTravailEmployeurToolBranchRegistry();

        assertThat(registry.knownBranches())
                .containsExactly("F-IM-46-autorisation-travail-employeur-fr:default");
    }
}
