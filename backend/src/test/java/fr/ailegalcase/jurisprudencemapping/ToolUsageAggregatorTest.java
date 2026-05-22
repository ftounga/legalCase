package fr.ailegalcase.jurisprudencemapping;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ToolUsageAggregatorTest {

    @Test
    void detectAll_emptyContributors_returnsEmpty() {
        ToolUsageAggregator agg = new ToolUsageAggregator(List.of());
        assertThat(agg.detectAll(UUID.randomUUID())).isEmpty();
    }

    @Test
    void detectAll_nullContributors_returnsEmpty() {
        ToolUsageAggregator agg = new ToolUsageAggregator(null);
        assertThat(agg.detectAll(UUID.randomUUID())).isEmpty();
    }

    @Test
    void detectAll_nullCaseFileId_returnsEmpty() {
        ToolUsageContributor c = stub("t1", new ToolUsage("t1", "b1"));
        ToolUsageAggregator agg = new ToolUsageAggregator(List.of(c));
        assertThat(agg.detectAll(null)).isEmpty();
    }

    @Test
    void detectAll_collectsAllContributors() {
        ToolUsageContributor c1 = stub("t1", new ToolUsage("t1", "b1"));
        ToolUsageContributor c2 = stub("t2", new ToolUsage("t2", "b2"));
        ToolUsageAggregator agg = new ToolUsageAggregator(List.of(c1, c2));

        List<ToolUsage> result = agg.detectAll(UUID.randomUUID());

        assertThat(result).extracting(ToolUsage::toolId).containsExactlyInAnyOrder("t1", "t2");
    }

    @Test
    void detectAll_skipsContributorReturningEmpty() {
        ToolUsageContributor c1 = stub("t1", new ToolUsage("t1", "b1"));
        ToolUsageContributor c2 = stubEmpty("t2");
        ToolUsageAggregator agg = new ToolUsageAggregator(List.of(c1, c2));

        List<ToolUsage> result = agg.detectAll(UUID.randomUUID());

        assertThat(result).hasSize(1).extracting(ToolUsage::toolId).containsExactly("t1");
    }

    @Test
    void detectAll_skipsContributorThrowingException() {
        ToolUsageContributor good = stub("t1", new ToolUsage("t1", "b1"));
        ToolUsageContributor bad = new ToolUsageContributor() {
            @Override public String toolId() { return "t-bad"; }
            @Override public Optional<ToolUsage> detectUsage(UUID id) { throw new RuntimeException("boom"); }
        };
        ToolUsageAggregator agg = new ToolUsageAggregator(List.of(bad, good));

        List<ToolUsage> result = agg.detectAll(UUID.randomUUID());

        assertThat(result).hasSize(1).extracting(ToolUsage::toolId).containsExactly("t1");
    }

    private ToolUsageContributor stub(String toolId, ToolUsage usage) {
        return new ToolUsageContributor() {
            @Override public String toolId() { return toolId; }
            @Override public Optional<ToolUsage> detectUsage(UUID id) { return Optional.of(usage); }
        };
    }

    private ToolUsageContributor stubEmpty(String toolId) {
        return new ToolUsageContributor() {
            @Override public String toolId() { return toolId; }
            @Override public Optional<ToolUsage> detectUsage(UUID id) { return Optional.empty(); }
        };
    }
}
