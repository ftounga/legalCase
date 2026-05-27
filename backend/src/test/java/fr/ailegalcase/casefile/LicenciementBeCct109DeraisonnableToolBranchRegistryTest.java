package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-JU-03 / SF-213-10 — vérifie que le registre déclare la branche
 * {@code licenciement-be-cct109-deraisonnable:default} attendue par le
 * mapping jurisprudence.
 */
class LicenciementBeCct109DeraisonnableToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new LicenciementBeCct109DeraisonnableToolBranchRegistry()
                .knownBranches())
                .containsExactly("licenciement-be-cct109-deraisonnable:default");
    }
}
