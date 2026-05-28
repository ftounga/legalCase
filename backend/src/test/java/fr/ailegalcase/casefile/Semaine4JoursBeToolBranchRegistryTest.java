package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-JU-03 / SF-219-18 — vérifie que le registre déclare la branche
 * {@code semaine-4-jours-be:default} attendue par le mapping
 * jurisprudence.
 */
class Semaine4JoursBeToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(
                new Semaine4JoursBeToolBranchRegistry().knownBranches())
                .containsExactly("semaine-4-jours-be:default");
    }
}
