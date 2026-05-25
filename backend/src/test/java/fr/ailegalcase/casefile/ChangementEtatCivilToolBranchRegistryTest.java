package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ChangementEtatCivilToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new ChangementEtatCivilToolBranchRegistry().knownBranches())
                .containsExactly("F-FA-26-changement-etat-civil:default");
    }
}
