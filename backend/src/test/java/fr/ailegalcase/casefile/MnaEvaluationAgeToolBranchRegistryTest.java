package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MnaEvaluationAgeToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        MnaEvaluationAgeToolBranchRegistry registry = new MnaEvaluationAgeToolBranchRegistry();

        assertThat(registry.knownBranches())
                .containsExactly("F-IM-38-mna-evaluation-age-fr:default");
    }
}
