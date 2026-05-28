package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-JU-03 / SF-219-31 — vérifie que le registre déclare la branche
 * {@code conge-paternite-naissance-be:default} attendue par le mapping
 * jurisprudence.
 */
class CongePaterniteNaissanceBeToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(
                new CongePaterniteNaissanceBeToolBranchRegistry()
                        .knownBranches())
                .containsExactly("conge-paternite-naissance-be:default");
    }
}
