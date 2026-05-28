package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-JU-03 / SF-219-22 — vérifie que le registre déclare la branche
 * {@code egalite-femmes-hommes-be:default} attendue par le mapping
 * jurisprudence.
 */
class EgaliteFemmesHommesBeToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(
                new EgaliteFemmesHommesBeToolBranchRegistry().knownBranches())
                .containsExactly("egalite-femmes-hommes-be:default");
    }
}
