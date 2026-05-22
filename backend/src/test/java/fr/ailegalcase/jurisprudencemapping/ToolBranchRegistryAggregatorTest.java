package fr.ailegalcase.jurisprudencemapping;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ToolBranchRegistryAggregatorTest {

    @Test
    void allKnownBranches_emptyList_returnsEmptySet() {
        ToolBranchRegistryAggregator agg = new ToolBranchRegistryAggregator(List.of());
        assertThat(agg.allKnownBranches()).isEmpty();
    }

    @Test
    void allKnownBranches_nullList_returnsEmptySet() {
        ToolBranchRegistryAggregator agg = new ToolBranchRegistryAggregator(null);
        assertThat(agg.allKnownBranches()).isEmpty();
    }

    @Test
    void allKnownBranches_unionOfMultipleRegistries() {
        ToolBranchRegistry r1 = () -> Set.of("t1:b1", "t1:b2");
        ToolBranchRegistry r2 = () -> Set.of("t2:b1", "t1:b1");
        ToolBranchRegistryAggregator agg = new ToolBranchRegistryAggregator(List.of(r1, r2));

        assertThat(agg.allKnownBranches()).containsExactlyInAnyOrder("t1:b1", "t1:b2", "t2:b1");
    }

    @Test
    void allKnownBranches_failingRegistry_isSkippedSafely() {
        ToolBranchRegistry good = () -> Set.of("t1:b1");
        ToolBranchRegistry bad = () -> { throw new RuntimeException("boom"); };
        ToolBranchRegistryAggregator agg = new ToolBranchRegistryAggregator(List.of(bad, good));

        Set<String> result = agg.allKnownBranches();
        assertThat(result).containsExactly("t1:b1");
    }

    @Test
    void allKnownBranches_nullSetFromRegistry_isHandled() {
        ToolBranchRegistry returnsNull = () -> null;
        ToolBranchRegistry good = () -> Set.of("t1:b1");
        ToolBranchRegistryAggregator agg = new ToolBranchRegistryAggregator(List.of(returnsNull, good));

        assertThat(agg.allKnownBranches()).containsExactly("t1:b1");
    }
}
