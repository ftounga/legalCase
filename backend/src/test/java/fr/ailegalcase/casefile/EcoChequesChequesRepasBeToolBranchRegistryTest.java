package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-JU-03 / SF-219-21 — vérifie que le registre déclare la branche
 * {@code eco-cheques-cheques-repas-be:default} attendue par le mapping
 * jurisprudence.
 */
class EcoChequesChequesRepasBeToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new EcoChequesChequesRepasBeToolBranchRegistry()
                .knownBranches())
                .containsExactly("eco-cheques-cheques-repas-be:default");
    }
}
