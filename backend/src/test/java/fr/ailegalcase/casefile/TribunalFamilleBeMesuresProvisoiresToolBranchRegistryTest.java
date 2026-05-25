package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class TribunalFamilleBeMesuresProvisoiresToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new TribunalFamilleBeMesuresProvisoiresToolBranchRegistry().knownBranches())
                .containsExactly("tribunal-famille-be-mesures-prov:default");
    }
}
