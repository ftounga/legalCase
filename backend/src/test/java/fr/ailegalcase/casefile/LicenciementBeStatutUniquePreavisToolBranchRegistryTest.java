package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-JU-03 / SF-213-03 — vérifie que le registre déclare la branche
 * {@code licenciement-be-statut-unique-preavis:default} attendue par le
 * mapping jurisprudence.
 */
class LicenciementBeStatutUniquePreavisToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new LicenciementBeStatutUniquePreavisToolBranchRegistry().knownBranches())
                .containsExactly("licenciement-be-statut-unique-preavis:default");
    }
}
