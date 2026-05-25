package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MediationFamilialePreSaisineToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new MediationFamilialePreSaisineToolBranchRegistry().knownBranches())
                .containsExactly("mediation-familiale-pre-saisine:default");
    }
}
