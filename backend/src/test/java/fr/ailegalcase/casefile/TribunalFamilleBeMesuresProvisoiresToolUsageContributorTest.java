package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TribunalFamilleBeMesuresProvisoiresToolUsageContributorTest {
    private TribunalFamilleBeMesuresProvisoiresRepository repository;
    private TribunalFamilleBeMesuresProvisoiresToolUsageContributor contributor;

    @BeforeEach
    void setUp() {
        repository = mock(TribunalFamilleBeMesuresProvisoiresRepository.class);
        contributor = new TribunalFamilleBeMesuresProvisoiresToolUsageContributor(repository);
    }

    @Test
    void toolId_returnsExpectedConstant() {
        assertThat(contributor.toolId()).isEqualTo("tribunal-famille-be-mesures-prov");
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
        when(repository.findByCaseFileId(caseFileId)).thenReturn(Optional.of(new TribunalFamilleBeMesuresProvisoiresAnalysis()));
        Optional<ToolUsage> result = contributor.detectUsage(caseFileId);
        assertThat(result).isPresent();
        assertThat(result.get().toolId()).isEqualTo("tribunal-famille-be-mesures-prov");
        assertThat(result.get().brancheCalculId()).isEqualTo("default");
    }
}
