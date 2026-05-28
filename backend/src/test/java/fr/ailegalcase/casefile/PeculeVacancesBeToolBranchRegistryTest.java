package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-JU-03 / SF-219-20 — vérifie que le registre déclare la branche
 * {@code pecule-vacances-be:default} attendue par le mapping
 * jurisprudence.
 */
class PeculeVacancesBeToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(
                new PeculeVacancesBeToolBranchRegistry().knownBranches())
                .containsExactly("pecule-vacances-be:default");
    }
}
