package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-JU-03 / SF-219-24 — vérifie que le registre déclare la branche
 * {@code code-penal-social-be:default} attendue par le mapping
 * jurisprudence.
 */
class CodePenalSocialBeToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(
                new CodePenalSocialBeToolBranchRegistry().knownBranches())
                .containsExactly("code-penal-social-be:default");
    }
}
