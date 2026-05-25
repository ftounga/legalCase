package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class C4OnemChecklistToolUsageContributorTest {
    private C4OnemChecklistRepository repository;
    private C4OnemChecklistToolUsageContributor contributor;

    @BeforeEach
    void setUp() {
        repository = mock(C4OnemChecklistRepository.class);
        contributor = new C4OnemChecklistToolUsageContributor(repository);
    }

    @Test
    void toolId_returnsExpectedConstant() {
        assertThat(contributor.toolId()).isEqualTo("c4-onem-checklist");
    }

    @Test
    void detectUsage_returnsEmpty_whenCaseFileIdNull() {
        assertThat(contributor.detectUsage(null)).isEmpty();
    }

    @Test
    void detectUsage_returnsEmpty_whenNoAnalysisExists() {
        UUID caseFileId = UUID.randomUUID();
        when(repository.findByCaseFileId(caseFileId)).thenReturn(Optional.empty());
        assertThat(contributor.detectUsage(caseFileId)).isEmpty();
    }

    @Test
    void detectUsage_returnsToolUsage_whenAnalysisExists() {
        UUID caseFileId = UUID.randomUUID();
        when(repository.findByCaseFileId(caseFileId)).thenReturn(Optional.of(new C4OnemChecklistAnalysis()));
        Optional<ToolUsage> result = contributor.detectUsage(caseFileId);
        assertThat(result).isPresent();
        assertThat(result.get().toolId()).isEqualTo("c4-onem-checklist");
        assertThat(result.get().brancheCalculId()).isEqualTo("default");
    }
}
