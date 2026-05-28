package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-JU-03 / SF-219-07 — vérifie que le registre déclare la branche
 * {@code licenciement-be-collectif-renault:default} attendue par le
 * mapping jurisprudence.
 */
class LicenciementBeCollectifRenaultToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new LicenciementBeCollectifRenaultToolBranchRegistry()
                .knownBranches())
                .containsExactly("licenciement-be-collectif-renault:default");
    }
}
