package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class MesuresEloignementToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new MesuresEloignementToolBranchRegistry().knownBranches())
                .containsExactly("F-IM-20-mesures-eloignement:default");
    }
}
