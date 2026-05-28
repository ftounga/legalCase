package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-JU-03 / SF-219-27 — vérifie que le registre déclare la branche
 * {@code inastri-statut-travailleur-independant:default} attendue par
 * le mapping jurisprudence.
 */
class InastriStatutTravailleurIndependantToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(
                new InastriStatutTravailleurIndependantToolBranchRegistry()
                        .knownBranches())
                .containsExactly(
                        "inastri-statut-travailleur-independant:default");
    }
}
