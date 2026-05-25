package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DivorceDcBeToolUsageContributorTest {
    private DivorceDcBeRepository repository;
    private DivorceDcBeToolUsageContributor contributor;

    @BeforeEach
    void setUp() {
        repository = mock(DivorceDcBeRepository.class);
        contributor = new DivorceDcBeToolUsageContributor(repository);
    }

    @Test
    void toolId_returnsExpectedConstant() {
        assertThat(contributor.toolId()).isEqualTo("divorce-dc-be");
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
        when(repository.findByCaseFileId(caseFileId)).thenReturn(Optional.of(new DivorceDcBeAnalysis()));
        Optional<ToolUsage> result = contributor.detectUsage(caseFileId);
        assertThat(result).isPresent();
        assertThat(result.get().toolId()).isEqualTo("divorce-dc-be");
        assertThat(result.get().brancheCalculId()).isEqualTo("default");
    }
}
