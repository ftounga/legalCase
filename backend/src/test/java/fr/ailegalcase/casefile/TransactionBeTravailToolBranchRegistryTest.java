package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-JU-03 / SF-213-06 — vérifie que le registre déclare la branche
 * {@code transaction-be-travail:default} attendue par le mapping
 * jurisprudence.
 */
class TransactionBeTravailToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new TransactionBeTravailToolBranchRegistry().knownBranches())
                .containsExactly("transaction-be-travail:default");
    }
}
