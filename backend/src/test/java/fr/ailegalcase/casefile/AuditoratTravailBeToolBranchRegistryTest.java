package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-JU-03 / SF-219-25 — vérifie que le registre déclare la branche
 * {@code auditorat-travail-be:default} attendue par le mapping
 * jurisprudence.
 */
class AuditoratTravailBeToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(
                new AuditoratTravailBeToolBranchRegistry().knownBranches())
                .containsExactly("auditorat-travail-be:default");
    }
}
