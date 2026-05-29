package fr.ailegalcase.casefile;

import fr.ailegalcase.jurisprudencemapping.ToolUsage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UeEeeSuisseSejourToolUsageContributorTest {

    private UeEeeSuisseSejourRepository repository;
    private UeEeeSuisseSejourToolUsageContributor contributor;

    @BeforeEach
    void setUp() {
        repository = mock(UeEeeSuisseSejourRepository.class);
        contributor = new UeEeeSuisseSejourToolUsageContributor(repository);
    }

    @Test
    void toolId_returnsExpectedConstant() {
        assertThat(contributor.toolId()).isEqualTo("F-IM-44-ue-eee-suisse-sejour-fr");
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
        when(repository.findByCaseFileId(caseFileId))
                .thenReturn(Optional.of(new UeEeeSuisseSejourAnalysis()));

        Optional<ToolUsage> result = contributor.detectUsage(caseFileId);

        assertThat(result).isPresent();
        assertThat(result.get().toolId()).isEqualTo("F-IM-44-ue-eee-suisse-sejour-fr");
        assertThat(result.get().brancheCalculId()).isEqualTo("default");
    }
}
