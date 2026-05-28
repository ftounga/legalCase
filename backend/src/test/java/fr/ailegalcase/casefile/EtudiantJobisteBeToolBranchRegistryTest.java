package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-JU-03 / SF-219-13 — vérifie que le registre déclare la branche
 * {@code etudiant-jobiste-be:default} attendue par le mapping
 * jurisprudence.
 */
class EtudiantJobisteBeToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new EtudiantJobisteBeToolBranchRegistry().knownBranches())
                .containsExactly("etudiant-jobiste-be:default");
    }
}
