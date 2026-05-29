package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AnefProcedureToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        AnefProcedureToolBranchRegistry registry = new AnefProcedureToolBranchRegistry();

        assertThat(registry.knownBranches())
                .containsExactly("F-IM-37-anef-procedure-fr:default");
    }
}
