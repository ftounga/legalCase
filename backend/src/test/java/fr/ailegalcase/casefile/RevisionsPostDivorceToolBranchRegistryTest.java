package fr.ailegalcase.casefile;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RevisionsPostDivorceToolBranchRegistryTest {
    @Test
    void knownBranches_returnsDefaultBranch() {
        assertThat(new RevisionsPostDivorceToolBranchRegistry().knownBranches())
                .containsExactly("F-FA-13-revisions-post-divorce:default");
    }
}
