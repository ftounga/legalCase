package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * F-JU-03 / SF-219-11 — vérifie que le registre déclare la branche
 * {@code conge-education-paye-region:default} attendue par le mapping
 * jurisprudence.
 */
class CongeEducationPayeRegionToolBranchRegistryTest {

    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new CongeEducationPayeRegionToolBranchRegistry()
                .knownBranches())
                .containsExactly("conge-education-paye-region:default");
    }
}
