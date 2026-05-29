package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AssignationResidenceToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        AssignationResidenceToolBranchRegistry registry = new AssignationResidenceToolBranchRegistry();

        assertThat(registry.knownBranches())
                .containsExactly("F-IM-42-assignation-residence-fr:default");
    }
}
