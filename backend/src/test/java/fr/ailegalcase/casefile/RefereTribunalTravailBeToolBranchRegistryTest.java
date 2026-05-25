package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RefereTribunalTravailBeToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new RefereTribunalTravailBeToolBranchRegistry().knownBranches())
                .containsExactly("refere-tribunal-travail-be:default");
    }
}
