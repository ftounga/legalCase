package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TribunalTravailFicheToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new TribunalTravailFicheToolBranchRegistry().knownBranches())
                .containsExactly("F-DT-06-requete-tribunal-travail:default");
    }
}
