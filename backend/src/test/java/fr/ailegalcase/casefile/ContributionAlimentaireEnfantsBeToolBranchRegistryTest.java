package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class ContributionAlimentaireEnfantsBeToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new ContributionAlimentaireEnfantsBeToolBranchRegistry().knownBranches())
                .containsExactly("contribution-alimentaire-enfants-be:default");
    }
}
