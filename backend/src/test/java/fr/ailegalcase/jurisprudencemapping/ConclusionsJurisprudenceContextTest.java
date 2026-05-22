package fr.ailegalcase.jurisprudencemapping;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConclusionsJurisprudenceContextTest {

    private ToolUsageAggregator aggregator;
    private ToolJurisprudenceService jurisprudenceService;
    private ConclusionsJurisprudenceContext context;

    @BeforeEach
    void setUp() {
        aggregator = mock(ToolUsageAggregator.class);
        jurisprudenceService = mock(ToolJurisprudenceService.class);
        context = new ConclusionsJurisprudenceContext(aggregator, jurisprudenceService);
    }

    @Test
    void collectForCaseFile_nullId_returnsEmpty() {
        assertThat(context.collectForCaseFile(null)).isEmpty();
    }

    @Test
    void collectForCaseFile_noUsages_returnsEmpty() {
        when(aggregator.detectAll(any())).thenReturn(List.of());
        assertThat(context.collectForCaseFile(UUID.randomUUID())).isEmpty();
    }

    @Test
    void collectForCaseFile_twoTools_returnsTwoEntries() {
        when(aggregator.detectAll(any())).thenReturn(List.of(
                new ToolUsage("t1", "b1"),
                new ToolUsage("t2", "b2")));
        when(jurisprudenceService.findByToolAndBranch("t1", "b1"))
                .thenReturn(List.of(citation("A1", "Arret 1")));
        when(jurisprudenceService.findByToolAndBranch("t2", "b2"))
                .thenReturn(List.of(citation("A2", "Arret 2")));

        List<ToolJurisprudenceCitationByTool> result = context.collectForCaseFile(UUID.randomUUID());

        assertThat(result).hasSize(2);
        assertThat(result).extracting(ToolJurisprudenceCitationByTool::toolId)
                .containsExactly("t1", "t2");
    }

    @Test
    void collectForCaseFile_dedupesByArretRef() {
        when(aggregator.detectAll(any())).thenReturn(List.of(
                new ToolUsage("t1", "b1"),
                new ToolUsage("t2", "b2")));
        ToolJurisprudenceCitationResponse same = citation("SAME-ID", "Cass. soc. 8 janv. 2025, n° 23-12.345");
        ToolJurisprudenceCitationResponse other = citation("OTHER", "Cass. soc. 5 avr. 2024, n° 22-99.999");
        when(jurisprudenceService.findByToolAndBranch("t1", "b1"))
                .thenReturn(List.of(same, other));
        when(jurisprudenceService.findByToolAndBranch("t2", "b2"))
                .thenReturn(List.of(same)); // duplicate

        List<ToolJurisprudenceCitationByTool> result = context.collectForCaseFile(UUID.randomUUID());

        // t1 garde les 2 ; t2 voit son seul arret dédupé → entrée vide → exclue
        assertThat(result).hasSize(1);
        assertThat(result.get(0).toolId()).isEqualTo("t1");
        assertThat(result.get(0).citations()).hasSize(2);
    }

    @Test
    void collectForCaseFile_emptyCitations_skipsTool() {
        when(aggregator.detectAll(any())).thenReturn(List.of(
                new ToolUsage("t1", "b1"),
                new ToolUsage("t2", "b2")));
        when(jurisprudenceService.findByToolAndBranch("t1", "b1")).thenReturn(List.of());
        when(jurisprudenceService.findByToolAndBranch("t2", "b2"))
                .thenReturn(List.of(citation("A2", "Arret 2")));

        List<ToolJurisprudenceCitationByTool> result = context.collectForCaseFile(UUID.randomUUID());

        assertThat(result).hasSize(1).extracting(ToolJurisprudenceCitationByTool::toolId).containsExactly("t2");
    }

    @Test
    void collectForCaseFile_serviceThrowsForOneTool_skipsAndContinues() {
        when(aggregator.detectAll(any())).thenReturn(List.of(
                new ToolUsage("t1", "b1"),
                new ToolUsage("t2", "b2")));
        when(jurisprudenceService.findByToolAndBranch("t1", "b1"))
                .thenThrow(new RuntimeException("boom"));
        when(jurisprudenceService.findByToolAndBranch("t2", "b2"))
                .thenReturn(List.of(citation("A2", "Arret 2")));

        List<ToolJurisprudenceCitationByTool> result = context.collectForCaseFile(UUID.randomUUID());

        assertThat(result).hasSize(1).extracting(ToolJurisprudenceCitationByTool::toolId).containsExactly("t2");
    }

    private ToolJurisprudenceCitationResponse citation(String id, String ref) {
        return new ToolJurisprudenceCitationResponse(
                UUID.randomUUID(), ref, "Cour de cassation",
                LocalDate.of(2025, 1, 8), "23-12.345",
                "https://www.legifrance.gouv.fr/" + id,
                "Chapeau " + id,
                Instant.now(),
                new BigDecimal("0.90"));
    }
}
