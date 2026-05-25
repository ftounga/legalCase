package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RecherchePaterniteToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new RecherchePaterniteToolBranchRegistry().knownBranches())
                .containsExactly("F-FA-18-recherche-paternite:default");
    }
}
