package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-JU-03 / SF-213-07 — vérifie que le registre déclare la branche
 * {@code harcelement-be-procedure-formelle:default} attendue par le mapping
 * jurisprudence.
 */
class HarcelementBeProcedureFormelleToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new HarcelementBeProcedureFormelleToolBranchRegistry().knownBranches())
                .containsExactly("harcelement-be-procedure-formelle:default");
    }
}
