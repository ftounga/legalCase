package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-JU-03 / SF-219-29 - verifie que le registre declare la branche
 * {@code at-mp-rente-capital-be:default} attendue par le mapping
 * jurisprudence.
 */
class AtMpRenteCapitalBeToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(
                new AtMpRenteCapitalBeToolBranchRegistry().knownBranches())
                .containsExactly("at-mp-rente-capital-be:default");
    }
}
