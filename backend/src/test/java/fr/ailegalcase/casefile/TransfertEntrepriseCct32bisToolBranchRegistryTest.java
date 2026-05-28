package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-JU-03 / SF-219-08 — vérifie que le registre déclare la branche
 * {@code transfert-entreprise-cct-32bis:default} attendue par le
 * mapping jurisprudence.
 */
class TransfertEntrepriseCct32bisToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new TransfertEntrepriseCct32bisToolBranchRegistry()
                .knownBranches())
                .containsExactly("transfert-entreprise-cct-32bis:default");
    }
}
