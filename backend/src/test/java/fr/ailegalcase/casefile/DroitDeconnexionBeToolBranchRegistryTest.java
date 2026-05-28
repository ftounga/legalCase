package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-JU-03 / SF-219-19 — vérifie que le registre déclare la branche
 * {@code droit-deconnexion-be:default} attendue par le mapping
 * jurisprudence.
 */
class DroitDeconnexionBeToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(
                new DroitDeconnexionBeToolBranchRegistry().knownBranches())
                .containsExactly("droit-deconnexion-be:default");
    }
}
