package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-JU-03 / SF-213-04 — vérifie que le registre déclare la branche
 * {@code licenciement-be-formule-claeys:default} attendue par le mapping
 * jurisprudence.
 */
class LicenciementBeFormuleClaeysToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new LicenciementBeFormuleClaeysToolBranchRegistry().knownBranches())
                .containsExactly("licenciement-be-formule-claeys:default");
    }
}
